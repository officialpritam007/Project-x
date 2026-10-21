-- Additive, bounded revision history for future text edits only.
-- Earlier edits cannot be reconstructed. All history is destroyed on delete-for-everyone.
create table private.message_edit_versions (
  message_id uuid not null references public.messages(id) on delete cascade,
  version int not null check(version between 1 and 1000000),
  old_body text not null check(char_length(old_body) between 1 and 4000),
  new_body text not null check(char_length(new_body) between 1 and 4000),
  edited_at timestamptz not null default now(),
  primary key(message_id,version)
);
alter table private.message_edit_versions enable row level security;
revoke all on private.message_edit_versions from public,anon,authenticated;
create index message_edit_versions_latest_idx
  on private.message_edit_versions(message_id,version desc);

create function private.capture_message_edit() returns trigger
language plpgsql security definer set search_path='' as $$
declare next_revision int;
begin
  if new.deleted_at is not null then
    -- Includes sender delete-for-everyone; no pre-delete text remains retrievable.
    delete from private.message_edit_versions where message_id=new.id;
  elsif new.body is distinct from old.body
     and new.edited_at is distinct from old.edited_at
     and old.deleted_at is null and new.media_id is null
  then
    select coalesce(max(version),0)+1 into next_revision
      from private.message_edit_versions where message_id=new.id;
    insert into private.message_edit_versions(message_id,version,old_body,new_body)
      values(new.id,next_revision,old.body,new.body);
    delete from private.message_edit_versions
      where message_id=new.id and version<=next_revision-10;
  end if;
  return new;
end $$;
revoke all on function private.capture_message_edit() from public,anon,authenticated;
create trigger capture_message_edit
  after update of body,edited_at,deleted_at on public.messages
  for each row execute function private.capture_message_edit();

create function private.message_edit_history(p_id uuid) returns jsonb
language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user();m public.messages;c public.conversations;result jsonb;
begin
  select * into m from public.messages where id=p_id;
  if m.id is null or m.deleted_at is not null or not private.is_member(m.conversation_id)
    or not private.message_visible(m.id)
  then raise exception 'Message unavailable' using errcode='42501';end if;
  select * into c from public.conversations where id=m.conversation_id;
  if private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end)
  then raise exception 'Contact unavailable' using errcode='42501';end if;
  select coalesce(jsonb_agg(to_jsonb(v) order by v.version desc),'[]')
    into result from (
      select version,old_body,new_body,edited_at
        from private.message_edit_versions
        where message_id=p_id order by version desc limit 10
    )v;
  return result;
end $$;
create function public.message_edit_history(p_id uuid) returns jsonb
language sql security invoker set search_path='' as $$
  select private.message_edit_history(p_id);
$$;
revoke all on function private.message_edit_history(uuid),public.message_edit_history(uuid)
  from public,anon,authenticated;
grant execute on function private.message_edit_history(uuid),public.message_edit_history(uuid)
  to authenticated;
