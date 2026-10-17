-- Extend the existing authenticated Cloudinary lifecycle; no public media URLs.
alter table private.images add column kind text not null default 'image' check(kind in('image','voice')),
 add column duration_ms int not null default 0 check(duration_ms between 0 and 120000);
alter table public.messages add column media_kind text not null default 'image' check(media_kind in('image','voice')),
 add column duration_ms int not null default 0 check(duration_ms between 0 and 120000);
create table private.moments(
 id uuid primary key, owner_id uuid not null references public.profiles(id),
 media_id uuid not null unique references private.images(id),
 kind text not null check(kind in('story','post')), caption text not null default '' check(char_length(caption)<=1000),
 archived boolean not null default false,created_at timestamptz not null default now(),expires_at timestamptz,
 check((kind='story' and expires_at is not null) or (kind='post' and expires_at is null))
);
alter table private.moments enable row level security;
revoke all on private.moments from public,anon,authenticated;
create index moments_owner_idx on private.moments(owner_id,kind,archived,created_at desc,id desc);
create index moments_feed_idx on private.moments(kind,created_at desc,id desc) where not archived;
create index moments_expiry_idx on private.moments(expires_at) where expires_at is not null;

create function private.moment_visible(p_owner uuid) returns boolean language sql stable security definer set search_path='' as $$
 select not private.is_blocked(p_owner) and exists(select 1 from public.contacts where owner_id=p_owner and target_id=auth.uid());
$$;
create function private.publish_moment(p_id uuid,p_media uuid,p_kind text,p_caption text) returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;m private.moments;begin
 if p_id is null or p_media is null or p_kind is null or p_kind not in('story','post') or char_length(coalesce(p_caption,''))>1000 then raise exception 'Invalid moment';end if;
 perform 1 from public.profiles where id=u for update;
 select * into m from private.moments where id=p_id;
 if m.id is not null then
 if m.owner_id<>u or m.media_id<>p_media or m.kind<>p_kind or m.caption<>coalesce(p_caption,'') then raise exception 'Moment conflict' using errcode='23505';end if;return m.id;end if;
 select * into i from private.images where id=p_media for update;
 if i.owner_id is distinct from u or not i.ready or i.destroyed or i.published or i.kind<>'image' or i.conversation_id is not null then raise exception 'Unavailable' using errcode='42501';end if;
 if (select count(*) from private.moments where owner_id=u)>=100 then raise exception 'Remove an older post or story first';end if;
 perform private.limit_action('moment',5);
 insert into private.moments(id,owner_id,media_id,kind,caption,expires_at) values(p_id,u,p_media,p_kind,coalesce(p_caption,''),case when p_kind='story' then now()+interval '24 hours' end);
 update private.images set published=true where id=i.id;return p_id;
end $$;
create function public.publish_moment(p_id uuid,p_media uuid,p_kind text,p_caption text default '') returns uuid language sql security invoker set search_path='' as $$select private.publish_moment(p_id,p_media,p_kind,p_caption)$$;
create function private.list_moments(p_kind text,p_mine boolean,p_archived boolean,p_before timestamptz,p_before_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();r jsonb;begin
 if p_kind is null or p_kind not in('story','post') then raise exception 'Invalid kind';end if;
 select coalesce(jsonb_agg(to_jsonb(t) order by t.created_at desc,t.id desc),'[]') into r from (
 select m.id,m.owner_id,m.media_id,m.kind,m.caption,m.created_at,m.expires_at,m.archived,p.display_name,p.username,p.avatar_id,i.width,i.height
 from private.moments m join public.profiles p on p.id=m.owner_id and not p.is_deleted
 join private.images i on i.id=m.media_id and i.ready and not i.destroyed
 where m.kind=p_kind and (m.created_at,m.id)<(p_before,p_before_id)
 and (m.expires_at is null or m.expires_at>now())
 and case when p_mine then m.owner_id=u and m.archived=p_archived else not m.archived and (m.owner_id=u or private.moment_visible(m.owner_id)) end
 order by m.created_at desc,m.id desc limit 30)t;return r;
end $$;
create function public.list_moments(p_kind text,p_mine boolean default false,p_archived boolean default false,p_before timestamptz default 'infinity',p_before_id uuid default 'ffffffff-ffff-ffff-ffff-ffffffffffff') returns jsonb language sql security invoker set search_path='' as $$select private.list_moments(p_kind,p_mine,p_archived,p_before,p_before_id)$$;
create function private.change_moment(p_id uuid,p_action text) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();m private.moments;begin
 select * into m from private.moments where id=p_id and owner_id=u for update;
 if m.id is null then raise exception 'Unavailable' using errcode='42501';end if;
 if p_action='delete' then
 delete from private.moments where id=m.id;update private.images set published=false where id=m.media_id;
 elsif p_action in('archive','restore') and m.kind='post' then update private.moments set archived=(p_action='archive') where id=m.id;
 else raise exception 'Invalid action';end if;
end $$;
create function public.change_moment(p_id uuid,p_action text) returns void language sql security invoker set search_path='' as $$select private.change_moment(p_id,p_action)$$;

create function private.prepare_voice(p_conversation uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;peer uuid;begin
 if p_conversation is null or not private.is_member(p_conversation) then raise exception 'Unavailable' using errcode='42501';end if;
 select case when user_a=u then user_b else user_a end into peer from public.conversations where id=p_conversation;
 if private.is_blocked(peer) then raise exception 'Unavailable' using errcode='42501';end if;
 perform private.limit_action('voice',5);
 insert into private.images(owner_id,conversation_id,public_id,kind) values(u,p_conversation,'liquid-chat/'||gen_random_uuid()::text,'voice') returning * into i;
 return jsonb_build_object('id',i.id,'public_id',i.public_id);
end $$;
create function public.prepare_voice(p_conversation uuid) returns jsonb language sql security invoker set search_path='' as $$select private.prepare_voice(p_conversation)$$;
create function private.finalize_voice(p_id uuid,p_format text,p_duration int,p_bytes int) returns void language plpgsql security definer set search_path='' as $$begin
 if p_format is null or p_format<>'m4a' or p_duration is null or p_duration not between 500 and 120000 or p_bytes is null or p_bytes not between 1 and 2097152 then raise exception 'Invalid voice';end if;
 update private.images set format=p_format,width=0,height=0,duration_ms=p_duration,bytes=p_bytes,ready=true where id=p_id and kind='voice' and not ready and not destroyed;
 if not found then raise exception 'Invalid upload';end if;
end $$;
create function public.finalize_voice(p_id uuid,p_format text,p_duration int,p_bytes int) returns void language sql security invoker set search_path='' as $$select private.finalize_voice(p_id,p_format,p_duration,p_bytes)$$;
create function private.send_voice(p_id uuid,p_media uuid) returns public.messages language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;c public.conversations;m public.messages;begin
 select * into i from private.images where id=p_media for update;
 if i.owner_id is distinct from u or i.kind<>'voice' or not i.ready or i.destroyed or i.conversation_id is null then raise exception 'Unavailable' using errcode='42501';end if;
 select * into c from public.conversations where id=i.conversation_id for update;
 if not private.is_member(c.id) then raise exception 'Unavailable' using errcode='42501';end if;
 select * into m from public.messages where id=p_id;
 if m.id is not null then
 if m.media_id is distinct from i.id or m.sender_id<>u or m.conversation_id<>c.id or m.media_kind<>'voice' then raise exception 'Message conflict' using errcode='23505';end if;return m;end if;
 if i.published or private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end) then raise exception 'Unavailable' using errcode='42501';end if;
 perform private.limit_action('send',60);
 insert into public.messages(id,conversation_id,sender_id,body,media_id,media_kind,duration_ms) values(p_id,c.id,u,'Voice message',i.id,'voice',i.duration_ms) returning * into m;
 update public.conversations set last_seq=m.seq where id=c.id;update private.images set published=true where id=i.id;return m;
end $$;
create function public.send_voice(p_id uuid,p_media uuid) returns public.messages language sql security invoker set search_path='' as $$select private.send_voice(p_id,p_media)$$;

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
 return jsonb_build_object('public_id',i.public_id,'format',i.format,'width',i.width,'height',i.height,'kind',i.kind,'duration_ms',i.duration_ms,'resource_type',case when i.kind='voice' then 'video' else 'image' end);
end $$;
-- Avatar and message image APIs cannot relabel or reuse published social/audio assets.
create or replace function private.set_avatar(p_media uuid) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 if p_media is not null and not exists(select 1 from private.images where id=p_media and owner_id=u and conversation_id is null and kind='image' and ready and not destroyed and (not published or id::text=(select avatar_id from public.profiles where id=u))) then raise exception 'Unavailable' using errcode='42501';end if;
 update private.images set published=false where owner_id=u and conversation_id is null and id::text=(select avatar_id from public.profiles where id=u) and id is distinct from p_media;
 update public.profiles set avatar_id=p_media::text where id=u;
 if p_media is not null then update private.images set published=true where id=p_media;end if;
end $$;
-- Limit all newly finalized media to 100 MB per account. A profile lock serializes finalizers.
create function private.media_quota() returns trigger language plpgsql security definer set search_path='' as $$begin
 if new.ready and not old.ready then
 perform 1 from public.profiles where id=new.owner_id for update;
 if (select coalesce(sum(bytes),0) from private.images where owner_id=new.owner_id and ready and not destroyed and id<>new.id)+coalesce(new.bytes,0)>104857600 then raise exception 'Media limit reached';end if;
 end if;return new;
end $$;
create trigger media_quota before update on private.images for each row execute function private.media_quota();
create or replace function private.garbage_images() returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;begin
 with expired as (delete from private.moments where expires_at<=now() returning media_id)
 update private.images set published=false where id in(select media_id from expired);
 with candidates as (select id from private.images where not published and not destroyed and created_at<now()-interval '1 hour' order by created_at for update skip locked limit 20), claimed as (update private.images i set ready=false from candidates c where i.id=c.id and not i.published returning i.id,i.public_id,case when i.kind='voice' then 'video' else 'image' end as resource_type)
 select coalesce(jsonb_agg(row_to_json(claimed)),'[]') into r from claimed;return r;
end $$;
create or replace function private.finish_garbage(p_id uuid) returns void language plpgsql security definer set search_path='' as $$begin
 delete from private.images where id=p_id and not published and not ready and not exists(select 1 from public.messages where media_id=p_id) and not exists(select 1 from public.profiles where avatar_id=p_id::text) and not exists(select 1 from private.moments where media_id=p_id);
end $$;
create or replace function private.deletion_batch(p_job uuid) returns jsonb language sql security definer set search_path='' as $$
 select jsonb_build_object('id',j.id,'user_id',j.user_id,'auth_exists',exists(select 1 from auth.users where id=j.user_id),'images',(select coalesce(jsonb_agg(to_jsonb(i)),'[]') from (select id,public_id,case when kind='voice' then 'video' else 'image' end as resource_type from private.images where owner_id=j.user_id and not destroyed limit 20)i)) from private.deletion_jobs j where j.state='pending' and (p_job is null or j.id=p_job) order by j.requested_at limit 1;
$$;
-- Profile erasure already tombstones the owner and denies reads. Clear social rows as well.
create function private.erase_moments() returns trigger language plpgsql security definer set search_path='' as $$begin
 if new.is_deleted and not old.is_deleted then delete from private.moments where owner_id=new.id;end if;return new;
end $$;
create trigger erase_moments after update of is_deleted on public.profiles for each row execute function private.erase_moments();
-- Always invoke cleanup so expired stories are processed even without orphan uploads.
select cron.schedule('liquid-media-cleanup','*/30 * * * *',$cron$
 select net.http_post(url:='https://qsdmspylumxysczzqnqc.supabase.co/functions/v1/media-cleanup',headers:=jsonb_build_object('Content-Type','application/json','apikey','sb_publishable_EIfZYiBXr4XR5eTW6vcPxw_qPLsQs78'),body:=jsonb_build_object('cron_secret',private.dispatch_secret())) where exists(select 1 from private.images where not published and not destroyed and created_at<now()-interval '1 hour') or exists(select 1 from private.moments where expires_at<=now());
$cron$);
-- Explicitly restrict every new routine before granting only public API wrappers and their private implementations.
revoke all on function private.moment_visible(uuid),private.publish_moment(uuid,uuid,text,text),private.list_moments(text,boolean,boolean,timestamptz,uuid),private.change_moment(uuid,text),private.prepare_voice(uuid),private.finalize_voice(uuid,text,int,int),private.send_voice(uuid,uuid),private.media_quota(),private.erase_moments() from public,anon,authenticated;
revoke all on function public.publish_moment(uuid,uuid,text,text),public.list_moments(text,boolean,boolean,timestamptz,uuid),public.change_moment(uuid,text),public.prepare_voice(uuid),public.finalize_voice(uuid,text,int,int),public.send_voice(uuid,uuid) from public,anon,authenticated;
grant execute on function private.publish_moment(uuid,uuid,text,text),private.list_moments(text,boolean,boolean,timestamptz,uuid),private.change_moment(uuid,text),private.prepare_voice(uuid),private.send_voice(uuid,uuid),public.publish_moment(uuid,uuid,text,text),public.list_moments(text,boolean,boolean,timestamptz,uuid),public.change_moment(uuid,text),public.prepare_voice(uuid),public.send_voice(uuid,uuid) to authenticated;
grant execute on function private.finalize_voice(uuid,text,int,int),public.finalize_voice(uuid,text,int,int) to service_role;
alter table private.images add constraint voice_ready_shape check(kind<>'voice' or not ready or (duration_ms between 500 and 120000 and format='m4a' and width=0 and height=0));
create function private.validate_message_media() returns trigger language plpgsql security definer set search_path='' as $$begin
 if new.media_id is not null and not exists(select 1 from private.images i where i.id=new.media_id and i.kind=new.media_kind and i.owner_id=new.sender_id and i.conversation_id=new.conversation_id and i.ready and not i.destroyed and (i.kind<>'voice' or i.duration_ms=new.duration_ms)) then raise exception 'Invalid media' using errcode='23514';end if;return new;
end $$;
create trigger validate_message_media before insert on public.messages for each row execute function private.validate_message_media();
revoke all on function private.validate_message_media() from public,anon,authenticated;
