-- Additive private PDF document messages. No changes to stored user data.
-- Deploy after 20261021183500_private_sticker_messages.sql and with media Edge update.
alter table private.images drop constraint images_kind_check;
alter table private.images add constraint images_kind_check
  check(kind in ('image','voice','story_video','sticker','document'));

alter table public.messages drop constraint messages_media_kind_check;
alter table public.messages add constraint messages_media_kind_check
  check(media_kind in ('image','voice','sticker','document'));

alter table private.images add constraint document_ready_shape
  check(kind<>'document' or not ready or
    (format='pdf' and width=0 and height=0 and duration_ms=0
      and bytes between 100 and 8388608));

update storage.buckets
  set allowed_mime_types=array['image/jpeg','image/png','audio/mp4','video/mp4','application/pdf']::text[]
  where id='liquid-media';

-- Reuse rate-limited, member-checked, blocked-contact-checked prepare_image.
-- For both stickers and documents the storage provider is private Supabase only.
create or replace function private.prepare_upload(p_conversation uuid,p_kind text,p_provider text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare intent jsonb;
begin
  if p_kind is null or p_provider is null or
    p_kind not in('image','voice','story_video','sticker','document')
    or p_provider not in('cloudinary','supabase')
    or (p_kind='story_video' and (p_conversation is not null or p_provider<>'supabase'))
    or (p_kind in('sticker','document') and (p_conversation is null or p_provider<>'supabase'))
  then raise exception 'Invalid upload' using errcode='42501';end if;
  if p_kind='voice' then intent:=private.prepare_voice(p_conversation);
  elsif p_kind='story_video' then intent:=private.prepare_story_video();
  else intent:=private.prepare_image(p_conversation);end if;
  update private.images set provider=p_provider,
    kind=case when p_kind in('sticker','document') then p_kind else kind end,
    format=case when p_provider='supabase' then
      case p_kind
        when 'voice' then 'm4a'
        when 'story_video' then 'mp4'
        when 'sticker' then 'png'
        when 'document' then 'pdf'
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
        when 'document' then 'pdf'
        else 'jpg' end
      else format end
  where id=p_id and not ready and not published and not destroyed
    and (kind not in('story_video','sticker','document') or p_provider='supabase');
  if not found then raise exception 'Invalid upload';end if;
end $$;

-- The Edge function checks PDF bytes before service-role finalization.
create function private.finalize_document(p_id uuid,p_bytes int)
returns void language plpgsql security definer set search_path='' as $$
begin
  if p_bytes not between 100 and 8388608 then raise exception 'Invalid PDF size';end if;
  update private.images set format='pdf',width=0,height=0,duration_ms=0,
    bytes=p_bytes,ready=true
  where id=p_id and kind='document' and provider='supabase'
    and conversation_id is not null and not ready and not published and not destroyed;
  if not found then raise exception 'PDF unavailable';end if;
end $$;
create function public.finalize_document(p_id uuid,p_bytes int)
returns void language sql security invoker set search_path='' as $$
  select private.finalize_document(p_id,p_bytes);
$$;
revoke all on function private.finalize_document(uuid,int),public.finalize_document(uuid,int)
  from public,anon,authenticated;
grant execute on function private.finalize_document(uuid,int),public.finalize_document(uuid,int)
  to service_role;

create function private.send_document(p_id uuid,p_media uuid,p_name text)
returns public.messages language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;c public.conversations;
  m public.messages;title text;
begin
  if p_id is null or p_media is null then raise exception 'Unavailable' using errcode='42501';end if;
  title:=regexp_replace(coalesce(p_name,''),'[^[:alnum:] _.()\\-]','','g');
  if title='' or length(title)>160 or title !~* '\.pdf$' then
    raise exception 'Invalid PDF filename';end if;
  select * into i from private.images where id=p_media for update;
  if i.owner_id is distinct from u or i.kind<>'document' or not i.ready
    or i.destroyed or i.conversation_id is null
  then raise exception 'Unavailable' using errcode='42501';end if;
  select * into c from public.conversations where id=i.conversation_id for update;
  if not private.is_member(c.id) then raise exception 'Unavailable' using errcode='42501';end if;
  select * into m from public.messages where id=p_id;
  if m.id is not null then
    if m.media_id is distinct from i.id or m.sender_id<>u
      or m.conversation_id<>c.id or m.media_kind<>'document'
      or m.body<>'Document: '||title
    then raise exception 'Message conflict' using errcode='23505';end if;
    return m;
  end if;
  if i.published or private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end)
  then raise exception 'Unavailable' using errcode='42501';end if;
  perform private.limit_action('send',60);
  insert into public.messages(id,conversation_id,sender_id,body,media_id,media_kind)
    values(p_id,c.id,u,'Document: '||title,i.id,'document') returning * into m;
  update public.conversations set last_seq=m.seq where id=c.id;
  update private.images set published=true where id=i.id;
  return m;
end $$;
create function public.send_document(p_id uuid,p_media uuid,p_name text)
returns public.messages language sql security invoker set search_path='' as $$
  select private.send_document(p_id,p_media,p_name);
$$;
revoke all on function private.send_document(uuid,uuid,text),public.send_document(uuid,uuid,text)
  from public,anon,authenticated;
grant execute on function private.send_document(uuid,uuid,text),public.send_document(uuid,uuid,text)
  to authenticated;
