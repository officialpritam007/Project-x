-- Additive, authenticated one-to-one PNG stickers.
-- Does not rewrite or delete existing messages, photos, calls or moments.
-- Deploy this migration together with the media Edge Function update.
alter table private.images drop constraint images_kind_check;
alter table private.images add constraint images_kind_check
  check(kind in ('image','voice','story_video','sticker'));

alter table public.messages drop constraint messages_media_kind_check;
alter table public.messages add constraint messages_media_kind_check
  check(media_kind in ('image','voice','sticker'));

alter table private.images add constraint sticker_ready_shape
  check(kind<>'sticker' or not ready or
    (format='png' and width=512 and height=512
      and bytes between 100 and 524288 and duration_ms=0));

update storage.buckets
  set allowed_mime_types=array['image/jpeg','image/png','audio/mp4','video/mp4']::text[]
  where id='liquid-media';

-- The caller must be a member and cannot be blocked in either direction.
-- Reuse the existing rate-limited prepare_image check.
create or replace function private.prepare_upload(p_conversation uuid,p_kind text,p_provider text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare intent jsonb;
begin
  if p_kind is null or p_provider is null
    or p_kind not in('image','voice','story_video','sticker')
    or p_provider not in('cloudinary','supabase')
    or (p_kind='story_video' and (p_conversation is not null or p_provider<>'supabase'))
    or (p_kind='sticker' and (p_conversation is null or p_provider<>'supabase'))
  then raise exception 'Invalid upload' using errcode='42501';end if;
  if p_kind='voice' then intent:=private.prepare_voice(p_conversation);
  elsif p_kind='story_video' then intent:=private.prepare_story_video();
  else intent:=private.prepare_image(p_conversation);
  end if;
  update private.images set provider=p_provider,
    kind=case when p_kind='sticker' then 'sticker' else kind end,
    format=case when p_provider='supabase' then
      case p_kind
        when 'voice' then 'm4a'
        when 'story_video' then 'mp4'
        when 'sticker' then 'png'
        else 'jpg' end
      else format end
  where id=(intent->>'id')::uuid;
  return intent;
end $$;

create or replace function private.set_media_provider(p_id uuid,p_provider text)
returns void language plpgsql security definer set search_path='' as $$
begin
  if p_provider is null or p_provider not in('cloudinary','supabase') then
    raise exception 'Invalid provider';end if;
  update private.images set provider=p_provider,
    format=case when p_provider='supabase' then
      case kind
        when 'voice' then 'm4a'
        when 'story_video' then 'mp4'
        when 'sticker' then 'png'
        else 'jpg' end
      else format end
  where id=p_id and not ready and not published and not destroyed
    and (kind not in('story_video','sticker') or p_provider='supabase');
  if not found then raise exception 'Invalid upload';end if;
end $$;

-- The Edge Function service role must validate actual PNG bytes first.
create function private.finalize_sticker(p_id uuid,p_bytes int)
returns void language plpgsql security definer set search_path='' as $$
begin
  if p_bytes not between 100 and 524288 then
    raise exception 'Invalid sticker size';end if;
  update private.images set format='png',width=512,height=512,
    duration_ms=0,bytes=p_bytes,ready=true
  where id=p_id and kind='sticker' and provider='supabase'
    and conversation_id is not null and not ready and not published and not destroyed;
  if not found then raise exception 'Sticker upload unavailable';end if;
end $$;

create function public.finalize_sticker(p_id uuid,p_bytes int)
returns void language sql security invoker set search_path='' as $$
  select private.finalize_sticker(p_id,p_bytes);
$$;
revoke all on function private.finalize_sticker(uuid,int),public.finalize_sticker(uuid,int)
  from public,anon,authenticated;
grant execute on function private.finalize_sticker(uuid,int),public.finalize_sticker(uuid,int)
  to service_role;

create function private.send_sticker(p_id uuid,p_media uuid)
returns public.messages language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;c public.conversations;m public.messages;
begin
  if p_id is null or p_media is null then raise exception 'Unavailable' using errcode='42501';end if;
  select * into i from private.images where id=p_media for update;
  if i.owner_id is distinct from u or i.kind<>'sticker' or not i.ready
    or i.destroyed or i.conversation_id is null
  then raise exception 'Unavailable' using errcode='42501';end if;
  select * into c from public.conversations where id=i.conversation_id for update;
  if not private.is_member(c.id) then raise exception 'Unavailable' using errcode='42501';end if;
  select * into m from public.messages where id=p_id;
  if m.id is not null then
    if m.media_id is distinct from i.id or m.sender_id<>u
      or m.conversation_id<>c.id or m.media_kind<>'sticker'
    then raise exception 'Message conflict' using errcode='23505';end if;
    return m;
  end if;
  if i.published or private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end)
  then raise exception 'Unavailable' using errcode='42501';end if;
  perform private.limit_action('send',60);
  insert into public.messages(id,conversation_id,sender_id,body,media_id,
      media_kind,image_width,image_height)
    values(p_id,c.id,u,'Sticker',i.id,'sticker',512,512) returning * into m;
  update public.conversations set last_seq=m.seq where id=c.id;
  update private.images set published=true where id=i.id;
  return m;
end $$;

create function public.send_sticker(p_id uuid,p_media uuid)
returns public.messages language sql security invoker set search_path='' as $$
  select private.send_sticker(p_id,p_media);
$$;
revoke all on function private.send_sticker(uuid,uuid),public.send_sticker(uuid,uuid)
  from public,anon,authenticated;
grant execute on function private.send_sticker(uuid,uuid),public.send_sticker(uuid,uuid)
  to authenticated;

-- Keep image sends strictly image-only; without this restriction a sticker
-- media intent could accidentally pass through send_image after this migration.
create or replace function private.send_image(p_id uuid,p_media uuid,p_caption text)
returns public.messages language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;m public.messages;
  c public.conversations;caption text:=coalesce(nullif(trim(p_caption),''),'Photo');
begin
  select * into i from private.images where id=p_media for update;
  if i.owner_id is distinct from u or i.kind<>'image'
    or not i.ready or i.destroyed or i.conversation_id is null
  then raise exception 'Unavailable' using errcode='42501';end if;
  select * into c from public.conversations where id=i.conversation_id for update;
  if not private.is_member(i.conversation_id) then
    raise exception 'Unavailable' using errcode='42501';end if;
  select * into m from public.messages where id=p_id;
  if m.id is not null then
    if m.media_id is distinct from i.id or m.sender_id<>u
      or m.conversation_id<>i.conversation_id or m.body<>caption
      or m.media_kind<>'image'
    then raise exception 'Message conflict' using errcode='23505';end if;
    return m;
  end if;
  if i.published or private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end)
  then raise exception 'Unavailable' using errcode='42501';end if;
  perform private.limit_action('send',60);
  insert into public.messages(id,conversation_id,sender_id,body,media_id,image_width,image_height)
    values(p_id,i.conversation_id,u,caption,i.id,i.width,i.height) returning * into m;
  update public.conversations set last_seq=m.seq where id=i.conversation_id;
  update private.images set published=true where id=i.id;
  return m;
end $$;

-- Existing media_access, deletion/garbage collection, quota and signed URL
-- functions already support image resource type for non-voice media.
-- Explicitly limit new private implementation privileges.
revoke all on function private.send_image(uuid,uuid,text) from public,anon;
