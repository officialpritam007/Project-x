-- Bounded, participant-only 30s H.264/AAC MP4 chat attachments.
-- Source-only. Apply after 20261021202500_message_edit_history.sql and
-- deploy the matching media Edge Function in a controlled release.
-- No existing messages or storage objects are rewritten/deleted.
alter table private.images drop constraint images_kind_check;
alter table private.images add constraint images_kind_check
  check(kind in ('image','voice','story_video','sticker','document','chat_video'));

alter table public.messages drop constraint messages_media_kind_check;
alter table public.messages add constraint messages_media_kind_check
  check(media_kind in ('image','voice','sticker','document','chat_video'));

alter table private.images add constraint chat_video_ready_shape
  check(kind<>'chat_video' or not ready or
    (provider='supabase' and format='mp4'
      and duration_ms between 500 and 30000
      and bytes between 100 and 8388608
      and least(width,height) between 1 and 720
      and greatest(width,height) between 1 and 1280));

-- Reuse the same rate-limited, member/blocked-peer checked image reservation.
-- The video is always stored in private Supabase, never a public URL.
create or replace function private.prepare_upload(p_conversation uuid,p_kind text,p_provider text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare intent jsonb;
begin
  if p_kind is null or p_provider is null
    or p_kind not in('image','voice','story_video','sticker','document','chat_video')
    or p_provider not in('cloudinary','supabase')
    or (p_kind='story_video' and (p_conversation is not null or p_provider<>'supabase'))
    or (p_kind in('sticker','document','chat_video')
         and (p_conversation is null or p_provider<>'supabase'))
  then raise exception 'Invalid upload' using errcode='42501';end if;
  if p_kind='voice' then intent:=private.prepare_voice(p_conversation);
  elsif p_kind='story_video' then intent:=private.prepare_story_video();
  else intent:=private.prepare_image(p_conversation);
  end if;
  update private.images set provider=p_provider,
    kind=case when p_kind in('sticker','document','chat_video') then p_kind else kind end,
    format=case when p_provider='supabase' then
      case p_kind when 'voice' then 'm4a' when 'story_video' then 'mp4'
        when 'sticker' then 'png' when 'document' then 'pdf'
        when 'chat_video' then 'mp4' else 'jpg' end else format end
  where id=(intent->>'id')::uuid;
  return intent;
end $$;

create or replace function private.set_media_provider(p_id uuid,p_provider text)
returns void language plpgsql security definer set search_path='' as $$
begin
  if p_provider is null or p_provider not in('cloudinary','supabase')
  then raise exception 'Invalid provider';end if;
  update private.images set provider=p_provider,
    format=case when p_provider='supabase' then
      case kind when 'voice' then 'm4a' when 'story_video' then 'mp4'
        when 'sticker' then 'png' when 'document' then 'pdf'
        when 'chat_video' then 'mp4' else 'jpg' end
      else format end
  where id=p_id and not ready and not published and not destroyed
    and (kind not in('story_video','sticker','document','chat_video')
         or p_provider='supabase');
  if not found then raise exception 'Invalid upload';end if;
end $$;

create function private.finalize_chat_video(p_id uuid,p_duration int,
  p_width int,p_height int,p_bytes int)
returns void language plpgsql security definer set search_path='' as $$
begin
  if p_duration not between 500 and 30000 or p_bytes not between 100 and 8388608
    or least(p_width,p_height) not between 1 and 720
    or greatest(p_width,p_height) not between 1 and 1280
  then raise exception 'Invalid chat video';end if;
  update private.images set format='mp4',width=p_width,height=p_height,
    duration_ms=p_duration,bytes=p_bytes,ready=true
  where id=p_id and kind='chat_video' and provider='supabase'
    and conversation_id is not null and not ready and not published and not destroyed;
  if not found then raise exception 'Video upload unavailable';end if;
end $$;
create function public.finalize_chat_video(p_id uuid,p_duration int,
  p_width int,p_height int,p_bytes int)
returns void language sql security invoker set search_path='' as $$
  select private.finalize_chat_video(p_id,p_duration,p_width,p_height,p_bytes);
$$;
revoke all on function private.finalize_chat_video(uuid,int,int,int,int),
  public.finalize_chat_video(uuid,int,int,int,int)
  from public,anon,authenticated;
grant execute on function private.finalize_chat_video(uuid,int,int,int,int),
  public.finalize_chat_video(uuid,int,int,int,int) to service_role;

create function private.send_chat_video(p_id uuid,p_media uuid,p_caption text)
returns public.messages language plpgsql security definer set search_path='' as $$
declare
  u uuid:=private.require_user();
  i private.images; c public.conversations; m public.messages;
  caption text:=coalesce(nullif(trim(p_caption),''),'Video');
begin
  if p_id is null or p_media is null or length(caption)>1000
  then raise exception 'Invalid video message';end if;
  select * into i from private.images where id=p_media for update;
  if i.owner_id is distinct from u or i.kind<>'chat_video'
    or not i.ready or i.destroyed or i.conversation_id is null
  then raise exception 'Unavailable' using errcode='42501';end if;
  select * into c from public.conversations where id=i.conversation_id for update;
  if not private.is_member(c.id) then raise exception 'Unavailable' using errcode='42501';end if;
  select * into m from public.messages where id=p_id;
  if m.id is not null then
    if m.media_id is distinct from i.id or m.sender_id<>u or
      m.conversation_id<>c.id or m.media_kind<>'chat_video' or
      m.body<>caption
    then raise exception 'Message conflict' using errcode='23505';end if;
    return m;
  end if;
  if i.published or private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end)
  then raise exception 'Unavailable' using errcode='42501';end if;
  perform private.limit_action('send',60);
  insert into public.messages(id,conversation_id,sender_id,body,media_id,
    media_kind,image_width,image_height,duration_ms)
  values(p_id,c.id,u,caption,i.id,'chat_video',i.width,i.height,i.duration_ms)
  returning * into m;
  update public.conversations set last_seq=m.seq where id=c.id;
  update private.images set published=true where id=i.id;
  return m;
end $$;

create function public.send_chat_video(p_id uuid,p_media uuid,p_caption text)
returns public.messages language sql security invoker set search_path='' as $$
  select private.send_chat_video(p_id,p_media,p_caption);
$$;
revoke all on function private.send_chat_video(uuid,uuid,text),
  public.send_chat_video(uuid,uuid,text) from public,anon,authenticated;
grant execute on function private.send_chat_video(uuid,uuid,text),
  public.send_chat_video(uuid,uuid,text) to authenticated;
