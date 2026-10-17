alter table private.images add column provider text not null default 'cloudinary' check(provider in('cloudinary','supabase'));
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types) values('liquid-media','liquid-media',false,10485760,array['image/jpeg','audio/mp4']) on conflict(id) do update set public=false,file_size_limit=excluded.file_size_limit,allowed_mime_types=excluded.allowed_mime_types;
create policy liquid_media_no_direct_access on storage.objects as restrictive for all to anon,authenticated using(bucket_id<>'liquid-media') with check(bucket_id<>'liquid-media');
-- Clients have no direct bucket policies. Service access happens only after the existing RPC checks.
create function private.set_media_provider(p_id uuid,p_provider text) returns void language plpgsql security definer set search_path='' as $$begin
 if p_provider is null or p_provider not in('cloudinary','supabase') then raise exception 'Invalid provider';end if;
 update private.images set provider=p_provider where id=p_id and not ready and not published and not destroyed;
 if not found then raise exception 'Invalid upload';end if;
end $$;
create function public.set_media_provider(p_id uuid,p_provider text) returns void language sql security invoker set search_path='' as $$select private.set_media_provider(p_id,p_provider)$$;
revoke all on function private.set_media_provider(uuid,text),public.set_media_provider(uuid,text) from public,anon,authenticated;
grant execute on function private.set_media_provider(uuid,text),public.set_media_provider(uuid,text) to service_role;
create or replace function private.media_quota() returns trigger language plpgsql security definer set search_path='' as $$begin
 if new.ready and not old.ready then
 perform 1 from public.profiles where id=new.owner_id for update;
 if (select coalesce(sum(bytes),0) from private.images where owner_id=new.owner_id and ready and not destroyed and id<>new.id)+coalesce(new.bytes,0)>104857600 then raise exception 'Media limit reached';end if;
 if new.provider='supabase' then
 perform pg_advisory_xact_lock(723451987);
 if (select coalesce(sum(bytes),0) from private.images where provider='supabase' and ready and not destroyed and id<>new.id)+coalesce(new.bytes,0)>536870912 then raise exception 'Shared media budget reached';end if;
 end if;
 end if;return new;
end $$;
create or replace function private.image_access(p_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;begin
 select * into i from private.images where id=p_id and ready and not destroyed;
 if i.id is null then raise exception 'Unavailable' using errcode='42501';end if;
 if i.owner_id<>u then
 if private.is_blocked(i.owner_id) then raise exception 'Unavailable' using errcode='42501';end if;
 if i.conversation_id is null then
 if not exists(select 1 from public.profiles p where p.id=i.owner_id and p.avatar_id=i.id::text and (exists(select 1 from public.conversations c where u in(c.user_a,c.user_b) and p.id in(c.user_a,c.user_b)) or exists(select 1 from public.contacts where owner_id=u and target_id=p.id)))
 and not exists(select 1 from private.moments m where m.media_id=i.id and not m.archived and (m.expires_at is null or m.expires_at>now()) and private.moment_visible(m.owner_id)) then raise exception 'Unavailable' using errcode='42501';end if;
 else
 if not private.is_member(i.conversation_id) or not exists(select 1 from public.messages m join public.conversation_state s on s.conversation_id=m.conversation_id and s.user_id=u where m.media_id=i.id and m.seq>s.deleted_seq) then raise exception 'Unavailable' using errcode='42501';end if;
 end if;
 end if;
 return jsonb_build_object('public_id',i.public_id,'format',i.format,'width',i.width,'height',i.height,'provider',i.provider,'kind',i.kind,'duration_ms',i.duration_ms,'resource_type',case when i.kind='voice' then 'video' else 'image' end);
end $$;
create or replace function private.garbage_images() returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;begin
 with expired as (delete from private.moments where expires_at<=now() returning media_id)
 update private.images set published=false where id in(select media_id from expired);
 with candidates as (select id from private.images where not published and not destroyed and created_at<now()-interval '1 hour' order by created_at for update skip locked limit 20), claimed as (update private.images i set ready=false from candidates c where i.id=c.id and not i.published returning i.id,i.public_id,i.provider,i.format,case when i.kind='voice' then 'video' else 'image' end as resource_type)
 select coalesce(jsonb_agg(row_to_json(claimed)),'[]') into r from claimed;return r;
end $$;
create or replace function private.deletion_batch(p_job uuid) returns jsonb language sql security definer set search_path='' as $$
 select jsonb_build_object('id',j.id,'user_id',j.user_id,'auth_exists',exists(select 1 from auth.users where id=j.user_id),'images',(select coalesce(jsonb_agg(to_jsonb(i)),'[]') from (select id,public_id,provider,format,case when kind='voice' then 'video' else 'image' end as resource_type from private.images where owner_id=j.user_id and not destroyed limit 20)i)) from private.deletion_jobs j where j.state='pending' and (p_job is null or j.id=p_job) order by j.requested_at limit 1;
$$;
