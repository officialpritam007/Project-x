-- Additive, private 30s video stories; existing photo/voice media and user data remain intact.
-- The upload Edge Function inspects actual MP4 samples before finalization.
alter table private.images drop constraint images_kind_check;
alter table private.images add constraint images_kind_check
  check(kind in('image','voice','story_video'));
alter table private.images add constraint story_video_ready_shape
  check(kind<>'story_video' or not ready or
    (format='mp4' and duration_ms between 500 and 30000 and bytes between 1 and 8388608
      and least(width,height) between 1 and 720
      and greatest(width,height) between 1 and 1280));

update storage.buckets set allowed_mime_types=array['image/jpeg','audio/mp4','video/mp4']::text[]
where id='liquid-media';

create function private.prepare_story_video() returns jsonb
language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;
begin
  perform private.limit_action('image',5);
  insert into private.images(owner_id,conversation_id,public_id,kind)
  values(u,null,'liquid-chat/'||gen_random_uuid()::text,'story_video') returning * into i;
  return jsonb_build_object('id',i.id,'public_id',i.public_id);
end $$;

create or replace function private.prepare_upload(p_conversation uuid,p_kind text,p_provider text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare intent jsonb;
begin
  if p_kind is null or p_provider is null or p_kind not in('image','voice','story_video') or p_provider not in('cloudinary','supabase')
    or (p_kind='story_video' and (p_conversation is not null or p_provider<>'supabase'))
  then raise exception 'Invalid upload' using errcode='42501'; end if;
  if p_kind='voice' then intent:=private.prepare_voice(p_conversation);
  elsif p_kind='story_video' then intent:=private.prepare_story_video();
  else intent:=private.prepare_image(p_conversation);
  end if;
  update private.images set provider=p_provider,
    format=case when p_provider='supabase' then
      case p_kind when 'voice' then 'm4a' when 'story_video' then 'mp4' else 'jpg' end
      else format end
  where id=(intent->>'id')::uuid;
  return intent;
end $$;

create or replace function private.set_media_provider(p_id uuid,p_provider text)
returns void language plpgsql security definer set search_path='' as $$
begin
  if p_provider is null or p_provider not in('cloudinary','supabase') then raise exception 'Invalid provider'; end if;
  update private.images set provider=p_provider,
    format=case when p_provider='supabase' then
      case kind when 'voice' then 'm4a' when 'story_video' then 'mp4' else 'jpg' end
      else format end
  where id=p_id and not ready and not published and not destroyed
    and (kind<>'story_video' or p_provider='supabase');
  if not found then raise exception 'Invalid upload'; end if;
end $$;

create function private.finalize_story_video(p_id uuid,p_duration int,p_width int,p_height int,p_bytes int)
returns void language plpgsql security definer set search_path='' as $$
begin
  if p_duration not between 500 and 30000 or p_bytes not between 1 and 8388608
     or least(p_width,p_height) not between 1 and 720
     or greatest(p_width,p_height) not between 1 and 1280
  then raise exception 'Invalid story video';end if;
  update private.images set format='mp4',duration_ms=p_duration,width=p_width,height=p_height,
    bytes=p_bytes,ready=true
  where id=p_id and kind='story_video' and provider='supabase' and not ready
    and not destroyed and not published and conversation_id is null;
  if not found then raise exception 'Invalid story video';end if;
end $$;
-- Only the Edge Function's service role can finalize after independent MP4 validation.
create function public.finalize_story_video(p_id uuid,p_duration int,p_width int,p_height int,p_bytes int)
returns void language sql security invoker set search_path='' as $$
  select private.finalize_story_video(p_id,p_duration,p_width,p_height,p_bytes);
$$;
revoke all on function private.prepare_story_video(),private.finalize_story_video(uuid,int,int,int,int),
  public.finalize_story_video(uuid,int,int,int,int) from public,anon,authenticated;
grant execute on function private.finalize_story_video(uuid,int,int,int,int),
  public.finalize_story_video(uuid,int,int,int,int) to service_role;

create or replace function private.publish_moment(p_id uuid,p_media uuid,p_kind text,p_caption text)
returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;m private.moments;
begin
  if p_id is null or p_media is null or p_kind is null or p_kind not in('story','post')
    or char_length(coalesce(p_caption,''))>1000 then raise exception 'Invalid moment';end if;
  perform 1 from public.profiles where id=u for update;
  select * into m from private.moments where id=p_id;
  if m.id is not null then
    if m.owner_id<>u or m.media_id<>p_media or m.kind<>p_kind
      or m.caption<>coalesce(p_caption,'')
    then raise exception 'Moment conflict' using errcode='23505';end if;
    return m.id;
  end if;
  select * into i from private.images where id=p_media for update;
  if i.owner_id is distinct from u or not i.ready or i.destroyed or i.published
    or i.conversation_id is not null
    or not (i.kind='image' or (p_kind='story' and i.kind='story_video'))
  then raise exception 'Unavailable' using errcode='42501';end if;
  if (select count(*) from private.moments where owner_id=u)>=100
  then raise exception 'Remove an older post or story first';end if;
  perform private.limit_action('moment',5);
  insert into private.moments(id,owner_id,media_id,kind,caption,expires_at)
    values(p_id,u,p_media,p_kind,coalesce(p_caption,''),
      case when p_kind='story' then now()+interval '24 hours' end);
  update private.images set published=true where id=i.id;
  return p_id;
end $$;

create or replace function private.list_moments(p_kind text,p_mine boolean,p_archived boolean,
  p_before timestamptz,p_before_id uuid)
returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();r jsonb;
begin
  if p_kind is null or p_kind not in('story','post') then raise exception 'Invalid kind';end if;
  select coalesce(jsonb_agg(to_jsonb(t) order by t.created_at desc,t.id desc),'[]') into r
  from (
    select m.id,m.owner_id,m.media_id,m.kind,m.caption,m.created_at,m.expires_at,
      m.archived,p.display_name,p.username,p.avatar_id,
      i.width,i.height,i.kind as media_kind,i.duration_ms
    from private.moments m
    join public.profiles p on p.id=m.owner_id and not p.is_deleted
    join private.images i on i.id=m.media_id and i.ready and not i.destroyed
    where m.kind=p_kind and (m.created_at,m.id)<(p_before,p_before_id)
      and (m.expires_at is null or m.expires_at>now())
      and case when p_mine then m.owner_id=u and m.archived=p_archived
        else not m.archived and (m.owner_id=u or private.moment_visible(m.owner_id)) end
    order by m.created_at desc,m.id desc limit 30
  )t;
  return r;
end $$;

-- Preserve the existing checks for messages, contacts and blocked audiences.
create or replace function private.image_access(p_id uuid)
returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;
begin
  select * into i from private.images where id=p_id and ready and not destroyed;
  if i.id is null then raise exception 'Unavailable' using errcode='42501';end if;
  if i.conversation_id is not null and i.published
    and not exists(select 1 from public.messages m where m.media_id=i.id
      and m.deleted_at is null and private.message_visible(m.id))
  then raise exception 'Unavailable' using errcode='42501';end if;
  if i.owner_id<>u then
    if private.is_blocked(i.owner_id) then raise exception 'Unavailable' using errcode='42501';end if;
    if i.conversation_id is null then
      if not exists(select 1 from public.profiles p
        where p.id=i.owner_id and p.avatar_id=i.id::text
          and (exists(select 1 from public.conversations c
            where u in(c.user_a,c.user_b) and p.id in(c.user_a,c.user_b))
            or exists(select 1 from public.contacts where owner_id=u and target_id=p.id)))
        and not exists(select 1 from private.moments m where m.media_id=i.id
          and not m.archived and (m.expires_at is null or m.expires_at>now())
          and private.moment_visible(m.owner_id))
      then raise exception 'Unavailable' using errcode='42501';end if;
    else
      if not private.is_member(i.conversation_id) or
        not exists(select 1 from public.messages m
          join public.conversation_state s on s.conversation_id=m.conversation_id and s.user_id=u
          where m.media_id=i.id and m.seq>s.deleted_seq)
      then raise exception 'Unavailable' using errcode='42501';end if;
    end if;
  end if;
  return jsonb_build_object('public_id',i.public_id,'format',i.format,'width',i.width,
    'height',i.height,'provider',i.provider,'kind',i.kind,'duration_ms',i.duration_ms,
    'resource_type',case when i.kind in('voice','story_video') then 'video' else 'image' end);
end $$;

create or replace function private.garbage_images()
returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;
begin
  with expired as (delete from private.moments where expires_at<=now() returning media_id)
    update private.images set published=false where id in(select media_id from expired);
  with candidates as (
    select id from private.images where not published and not destroyed
      and created_at<now()-interval '1 hour'
    order by created_at for update skip locked limit 20
  ), claimed as (
    update private.images i set ready=false from candidates c
    where i.id=c.id and not i.published
    returning i.id,i.public_id,i.provider,i.format,
      case when i.kind in('voice','story_video') then 'video' else 'image' end as resource_type
  ) select coalesce(jsonb_agg(row_to_json(claimed)),'[]') into r from claimed;
  return r;
end $$;
