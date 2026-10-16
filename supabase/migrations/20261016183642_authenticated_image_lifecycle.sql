create table private.images(
 id uuid primary key default gen_random_uuid(), owner_id uuid not null references public.profiles(id),
 conversation_id uuid references public.conversations(id),public_id text not null unique,
 format text,width int,height int,bytes int,ready boolean not null default false,published boolean not null default false,
 created_at timestamptz not null default now()
);
alter table private.images enable row level security;
create policy images_internal on private.images for all to authenticated using(false) with check(false);
alter table public.messages add column media_id uuid references private.images(id), add column image_width int not null default 0, add column image_height int not null default 0;
create function private.prepare_image(p_conversation uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;peer uuid;begin
 if p_conversation is not null then
 if not private.is_member(p_conversation) then raise exception 'Unavailable' using errcode='42501';end if;
 select case when user_a=u then user_b else user_a end into peer from public.conversations where id=p_conversation;
 if private.is_blocked(peer) then raise exception 'Unavailable' using errcode='42501';end if;
 end if;
 perform private.limit_action('image',5);
 insert into private.images(owner_id,conversation_id,public_id) values(u,p_conversation,'liquid-chat/'||gen_random_uuid()::text) returning * into i;
 return jsonb_build_object('id',i.id,'public_id',i.public_id);
end $$;
create function public.prepare_image(p_conversation uuid default null) returns jsonb language sql security invoker set search_path='' as $$select private.prepare_image(p_conversation)$$;
create function private.finalize_image(p_id uuid,p_format text,p_width int,p_height int,p_bytes int) returns void language plpgsql security definer set search_path='' as $$
begin
 if p_format not in('jpg','png','webp') or p_width<1 or p_height<1 or p_width::bigint*p_height>25000000 or p_bytes<1 or p_bytes>10485760 then raise exception 'Invalid image';end if;
 update private.images set format=p_format,width=p_width,height=p_height,bytes=p_bytes,ready=true where id=p_id and not ready;
 if not found then raise exception 'Invalid upload';end if;
end $$;
create function public.finalize_image(p_id uuid,p_format text,p_width int,p_height int,p_bytes int) returns void language sql security invoker set search_path='' as $$select private.finalize_image(p_id,p_format,p_width,p_height,p_bytes)$$;
create function private.send_image(p_id uuid,p_media uuid,p_caption text) returns public.messages language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;m public.messages;begin
 select * into i from private.images where id=p_media for update;
 if i.owner_id is distinct from u or not i.ready or i.conversation_id is null then raise exception 'Unavailable' using errcode='42501';end if;
 if i.published then
 select * into m from public.messages where id=p_id and media_id=i.id and sender_id=u;
 if m.id is null then raise exception 'Image already used';end if;return m;
 end if;
 m:=private.send_text(p_id,i.conversation_id,coalesce(nullif(trim(p_caption),''),'Photo'));
 if m.media_id is not null and m.media_id<>i.id then raise exception 'Message conflict';end if;
 update public.messages set media_id=i.id,image_width=i.width,image_height=i.height where id=m.id returning * into m;
 update private.images set published=true where id=i.id;
 return m;
end $$;
create function public.send_image(p_id uuid,p_media uuid,p_caption text default '') returns public.messages language sql security invoker set search_path='' as $$select private.send_image(p_id,p_media,p_caption)$$;
create function private.set_avatar(p_media uuid) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 if p_media is null then update public.profiles set avatar_id=null where id=u;return;end if;
 if not exists(select 1 from private.images where id=p_media and owner_id=u and conversation_id is null and ready) then raise exception 'Unavailable' using errcode='42501';end if;
 update public.profiles set avatar_id=p_media::text where id=u;
 update private.images set published=true where id=p_media;
end $$;
create function public.set_avatar(p_media uuid default null) returns void language sql security invoker set search_path='' as $$select private.set_avatar(p_media)$$;
create function private.image_access(p_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;begin
 select * into i from private.images where id=p_id and ready;
 if i.id is null then raise exception 'Unavailable' using errcode='42501';end if;
 if i.owner_id<>u then
 if i.conversation_id is null then
 if not exists(select 1 from public.profiles p join public.conversations c on p.id in(c.user_a,c.user_b) where p.avatar_id=i.id::text and u in(c.user_a,c.user_b)) or private.is_blocked(i.owner_id) then raise exception 'Unavailable' using errcode='42501';end if;
 else
 if private.is_blocked(i.owner_id) or not private.is_member(i.conversation_id) or not exists(select 1 from public.messages m join public.conversation_state s on s.conversation_id=m.conversation_id and s.user_id=u where m.media_id=i.id and m.seq>s.deleted_seq) then raise exception 'Unavailable' using errcode='42501';end if;
 end if;
 end if;
 return jsonb_build_object('public_id',i.public_id,'format',i.format,'width',i.width,'height',i.height);
end $$;
create function public.image_access(p_id uuid) returns jsonb language sql security invoker set search_path='' as $$select private.image_access(p_id)$$;
revoke all on function private.prepare_image(uuid),private.send_image(uuid,uuid,text),private.set_avatar(uuid),private.image_access(uuid),private.finalize_image(uuid,text,int,int,int) from public,anon,authenticated;
revoke all on function public.prepare_image(uuid),public.send_image(uuid,uuid,text),public.set_avatar(uuid),public.image_access(uuid),public.finalize_image(uuid,text,int,int,int) from public,anon,authenticated;
grant execute on function private.prepare_image(uuid),private.send_image(uuid,uuid,text),private.set_avatar(uuid),private.image_access(uuid),public.prepare_image(uuid),public.send_image(uuid,uuid,text),public.set_avatar(uuid),public.image_access(uuid) to authenticated;
grant usage on schema private to service_role;
grant execute on function private.finalize_image(uuid,text,int,int,int),public.finalize_image(uuid,text,int,int,int) to service_role;
