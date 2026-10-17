-- Additive modern actions. Every writer locks its conversation before allocating change_seq.
-- This makes the per-conversation mutation cursor safe across concurrent commits.
create sequence private.message_change_seq;
revoke all on sequence private.message_change_seq from public,anon,authenticated;
alter table public.messages
 add column reply_to_id uuid references public.messages(id),
 add column edited_at timestamptz,
 add column deleted_at timestamptz,
 add column reactions jsonb not null default '{}'::jsonb,
 add column change_seq bigint not null default 0;
create index messages_changes_idx on public.messages(conversation_id,change_seq) where change_seq>0;
create index messages_reply_idx on public.messages(reply_to_id) where reply_to_id is not null;
alter table public.conversation_state add column pinned_at timestamptz,add column manual_unread boolean not null default false;
create table private.message_preferences (
 user_id uuid not null references public.profiles(id) on delete cascade,
 message_id uuid not null references public.messages(id) on delete cascade,
 hidden boolean not null default false,starred boolean not null default false,
 primary key(user_id,message_id)
);
alter table private.message_preferences enable row level security;
revoke all on private.message_preferences from public,anon,authenticated;

create function private.message_visible(p_id uuid) returns boolean language sql stable security definer set search_path='' as $$
 select private.session_active() and exists(
 select 1 from public.messages m join public.conversation_state s on s.conversation_id=m.conversation_id and s.user_id=auth.uid()
 where m.id=p_id and private.is_member(m.conversation_id) and m.seq>s.deleted_seq
 and not exists(select 1 from private.message_preferences p where p.message_id=m.id and p.user_id=auth.uid() and p.hidden));
$$;
revoke all on function private.message_visible(uuid) from public,anon;
grant execute on function private.message_visible(uuid) to authenticated;
create policy hidden_messages on public.messages as restrictive for select to authenticated using(private.message_visible(id));

create function private.message_snapshot(p_id uuid) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user();m public.messages;begin
 select * into m from public.messages where id=p_id;
 if m.id is null or not private.is_member(m.conversation_id) then raise exception 'Message unavailable' using errcode='42501';end if;
 if not private.message_visible(p_id) then return jsonb_build_object('id',m.id,'hidden',true,'change_seq',m.change_seq);end if;
 return to_jsonb(m)||jsonb_build_object('starred',coalesce((select starred from private.message_preferences p where p.user_id=u and p.message_id=m.id),false));
end $$;
revoke all on function private.message_snapshot(uuid) from public,anon,authenticated;

create function private.message_history(p_conversation uuid,p_before bigint,p_after bigint,p_desc boolean) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare result jsonb;u uuid:=private.require_user();begin
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 select coalesce(jsonb_agg(private.message_snapshot(t.id) order by case when p_desc then -t.seq else t.seq end),'[]') into result from (
 select m.id,m.seq from public.messages m where m.conversation_id=p_conversation and m.seq<p_before and m.seq>p_after and private.message_visible(m.id)
 order by case when p_desc then -m.seq else m.seq end limit 100) t;
 return result;
end $$;
create function public.message_history(p_conversation uuid,p_before bigint default 9223372036854775807,p_after bigint default 0,p_desc boolean default true) returns jsonb language sql security invoker set search_path='' as $$select private.message_history(p_conversation,p_before,p_after,p_desc)$$;

create function private.message_changes(p_conversation uuid,p_after bigint) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user();result jsonb;begin
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 select coalesce(jsonb_agg(private.message_snapshot(t.id) order by t.change_seq),'[]') into result from (
 select m.id,m.change_seq from public.messages m where m.conversation_id=p_conversation and m.change_seq>greatest(p_after,0)
 order by m.change_seq limit 100) t;
 return result;
end $$;
create function public.message_changes(p_conversation uuid,p_after bigint default 0) returns jsonb language sql security invoker set search_path='' as $$select private.message_changes(p_conversation,p_after)$$;

create function private.message_action(p_id uuid,p_action text,p_value text) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();m public.messages;c public.conversations;peer uuid;begin
 select * into m from public.messages where id=p_id;
 if m.id is null then raise exception 'Message unavailable' using errcode='42501';end if;
 select * into c from public.conversations where id=m.conversation_id for update;
 select * into m from public.messages where id=p_id for update;
 if not private.message_visible(p_id) then raise exception 'Message unavailable' using errcode='42501';end if;
 peer:=case when c.user_a=u then c.user_b else c.user_a end;
 if private.is_blocked(peer) and p_action not in('hide','star') then raise exception 'Contact unavailable' using errcode='42501';end if;
 perform private.limit_action('message_action',60);
 case p_action
 when 'edit' then
  if m.sender_id<>u or m.deleted_at is not null or m.media_id is not null or now()>m.created_at+interval '15 minutes' then raise exception 'Message cannot be edited' using errcode='42501';end if;
  if char_length(trim(p_value)) not between 1 and 4000 then raise exception 'Invalid message' using errcode='22023';end if;
  update public.messages set body=p_value,edited_at=now() where id=p_id;
 when 'delete' then
  if m.sender_id<>u or now()>m.created_at+interval '48 hours' then raise exception 'Message cannot be deleted for everyone' using errcode='42501';end if;
  update public.messages set body='This message was deleted',deleted_at=coalesce(deleted_at,now()),media_id=null,image_width=0,image_height=0,duration_ms=0,reactions='{}',reply_to_id=null where id=p_id;
  delete from private.message_preferences where message_id=p_id;
 when 'react' then
  if m.deleted_at is not null or p_value not in('👍','❤️','😂','😮','😢','🙏','👏','') then raise exception 'Invalid reaction' using errcode='22023';end if;
  update public.messages set reactions=case when p_value='' then reactions-u::text else jsonb_set(reactions,array[u::text],to_jsonb(p_value)) end where id=p_id;
 when 'hide' then
  insert into private.message_preferences(user_id,message_id,hidden) values(u,p_id,true) on conflict(user_id,message_id) do update set hidden=true,starred=false;
 when 'star' then
  if m.deleted_at is not null or p_value not in('true','false') then raise exception 'Message unavailable' using errcode='22023';end if;
  insert into private.message_preferences(user_id,message_id,starred) values(u,p_id,p_value::boolean) on conflict(user_id,message_id) do update set starred=excluded.starred;
 else raise exception 'Unknown action' using errcode='22023';
 end case;
 update public.messages set change_seq=nextval('private.message_change_seq') where id=p_id;
 -- Existing realtime subscription receives a participant-authorized invalidation hint.
 update public.conversations set last_seq=last_seq where id=c.id;
 return private.message_snapshot(p_id);
end $$;
create function public.message_action(p_id uuid,p_action text,p_value text default '') returns jsonb language sql security invoker set search_path='' as $$select private.message_action(p_id,p_action,p_value)$$;

create function private.send_reply(p_id uuid,p_conversation uuid,p_body text,p_reply uuid) returns public.messages language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();m public.messages;r public.messages;c public.conversations;begin
 select * into c from public.conversations where id=p_conversation for update;
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 select * into m from public.messages where id=p_id;
 if m.id is not null then
  if m.sender_id<>u or m.conversation_id<>p_conversation or m.reply_to_id is distinct from p_reply or m.body<>p_body then raise exception 'Message ID conflict' using errcode='23505';end if;
  return m;
 end if;
 select * into r from public.messages where id=p_reply;
 if not private.message_visible(p_reply) or r.conversation_id<>p_conversation or r.deleted_at is not null then raise exception 'Reply unavailable' using errcode='42501';end if;
 if private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end) then raise exception 'Contact unavailable' using errcode='42501';end if;
 if char_length(trim(p_body)) not between 1 and 4000 then raise exception 'Invalid message' using errcode='22023';end if;
 perform private.limit_action('send',60);
 insert into public.messages(id,conversation_id,sender_id,body,reply_to_id) values(p_id,p_conversation,u,p_body,p_reply) returning * into m;
 update public.conversations set last_seq=m.seq where id=c.id;
 return m;
end $$;
create function public.send_reply(p_id uuid,p_conversation uuid,p_body text,p_reply uuid) returns public.messages language sql security invoker set search_path='' as $$select private.send_reply(p_id,p_conversation,p_body,p_reply)$$;

create function private.chat_action(p_conversation uuid,p_action text,p_enabled boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 -- Serialize the three-pin budget across this account, including different conversations.
 perform pg_advisory_xact_lock(hashtextextended(u::text,0));
 perform 1 from public.conversations where id=p_conversation for update;
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 perform private.limit_action('chat_action',30);
 if p_action='pin' then
  if p_enabled and not exists(select 1 from public.conversation_state where user_id=u and conversation_id=p_conversation and pinned_at is not null)
   and (select count(*) from public.conversation_state where user_id=u and pinned_at is not null)>=3 then raise exception 'You can pin up to 3 chats' using errcode='22023';end if;
  update public.conversation_state set pinned_at=case when p_enabled then coalesce(pinned_at,now()) else null end where user_id=u and conversation_id=p_conversation;
 elsif p_action='unread' then update public.conversation_state set manual_unread=p_enabled where user_id=u and conversation_id=p_conversation;
 else raise exception 'Unknown action' using errcode='22023';end if;
end $$;
create function public.chat_action(p_conversation uuid,p_action text,p_enabled boolean) returns void language sql security invoker set search_path='' as $$select private.chat_action(p_conversation,p_action,p_enabled)$$;

-- Clear manual unread only when the user opens/reads the conversation; preserve true receipt watermarks.
create function private.clear_manual_unread() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.read_seq>old.read_seq then new.manual_unread=false;end if;return new;
end $$;
revoke all on function private.clear_manual_unread() from public,anon,authenticated;
create trigger clear_manual_unread before update of read_seq on public.conversation_state for each row execute function private.clear_manual_unread();

-- No generic table writes and no anonymous function execution.
revoke all on function private.message_history(uuid,bigint,bigint,boolean),public.message_history(uuid,bigint,bigint,boolean),private.message_changes(uuid,bigint),public.message_changes(uuid,bigint),private.message_action(uuid,text,text),public.message_action(uuid,text,text),private.send_reply(uuid,uuid,text,uuid),public.send_reply(uuid,uuid,text,uuid),private.chat_action(uuid,text,boolean),public.chat_action(uuid,text,boolean) from public,anon;
grant execute on function private.message_history(uuid,bigint,bigint,boolean),public.message_history(uuid,bigint,bigint,boolean),private.message_changes(uuid,bigint),public.message_changes(uuid,bigint),private.message_action(uuid,text,text),public.message_action(uuid,text,text),private.send_reply(uuid,uuid,text,uuid),public.send_reply(uuid,uuid,text,uuid),private.chat_action(uuid,text,boolean),public.chat_action(uuid,text,boolean) to authenticated;

create or replace function private.list_chats(p_before_seq bigint,p_before_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); result jsonb;
begin
 select coalesce(jsonb_agg(row_to_json(t)::jsonb order by t.last_seq desc,t.id desc),'[]') into result from (
 select c.id,c.last_seq,c.user_a,c.user_b,p.username,p.display_name,p.avatar_id,
 s.favorite,s.archived,s.muted,s.deleted_seq,s.read_seq,s.pinned_at,s.manual_unread,
 peer.delivered_seq as peer_delivered_seq,peer.read_seq as peer_read_seq,
 greatest((select count(*) from public.messages m where m.conversation_id=c.id and m.sender_id<>u and m.seq>greatest(s.read_seq,s.deleted_seq) and m.deleted_at is null and not exists(select 1 from private.message_preferences x where x.user_id=u and x.message_id=m.id and x.hidden)),case when s.manual_unread then 1 else 0 end) as unread,
 (select m.created_at from public.messages m where m.conversation_id=c.id and m.seq>s.deleted_seq and not exists(select 1 from private.message_preferences x where x.user_id=u and x.message_id=m.id and x.hidden) order by m.seq desc limit 1) as last_at,
 (select left(m.body,160) from public.messages m where m.conversation_id=c.id and m.seq>s.deleted_seq and not exists(select 1 from private.message_preferences x where x.user_id=u and x.message_id=m.id and x.hidden) order by m.seq desc limit 1) as preview
 from public.conversations c
 join public.conversation_state s on s.conversation_id=c.id and s.user_id=u
 join public.profiles p on p.id=case when c.user_a=u then c.user_b else c.user_a end
 join public.conversation_state peer on peer.conversation_id=c.id and peer.user_id=p.id
 where u in(c.user_a,c.user_b) and (c.last_seq,c.id)<(p_before_seq,p_before_id)
 and (c.last_seq>s.deleted_seq or c.last_seq=0)
 order by c.last_seq desc,c.id desc limit 100
 ) t;
 return result;
end $$;


create or replace function private.image_access(p_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;begin
 select * into i from private.images where id=p_id and ready and not destroyed;
 if i.id is null then raise exception 'Unavailable' using errcode='42501';end if;
 if i.conversation_id is not null and i.published and not exists(select 1 from public.messages m where m.media_id=i.id and m.deleted_at is null and private.message_visible(m.id)) then raise exception 'Unavailable' using errcode='42501';end if;
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
create function public.message_by_id(p_id uuid) returns jsonb language sql security invoker set search_path='' as $$select private.message_snapshot(p_id)$$;
-- The wrapper exposes only the session-checked, participant-scoped snapshot.
revoke all on function public.message_by_id(uuid) from public,anon;
grant execute on function public.message_by_id(uuid),private.message_snapshot(uuid) to authenticated;

create function private.notification_preview(p_id uuid) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user();r jsonb;begin
 select jsonb_build_object('conversation_id',m.conversation_id,'title',case when s.notification_previews then p.display_name else 'Liquid Chat' end,
 'body',case when s.notification_previews then left(m.body,180) else 'New message' end,'previews',s.notification_previews) into r
 from public.messages m join public.profiles p on p.id=m.sender_id join public.settings s on s.user_id=u
 join public.conversation_state cs on cs.user_id=u and cs.conversation_id=m.conversation_id
 where m.id=p_id and m.sender_id<>u and m.deleted_at is null and private.message_visible(m.id) and not cs.muted and not private.is_blocked(m.sender_id);
 return r;
end $$;
create function public.notification_preview(p_id uuid) returns jsonb language sql security invoker set search_path='' as $$select private.notification_preview(p_id)$$;
revoke all on function private.notification_preview(uuid),public.notification_preview(uuid) from public,anon;
grant execute on function private.notification_preview(uuid),public.notification_preview(uuid) to authenticated;

create function private.shared_media(p_conversation uuid,p_before bigint,p_kind text) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user();c public.conversations;r jsonb;begin
 select * into c from public.conversations where id=p_conversation;
 if not private.is_member(p_conversation) or private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 select coalesce(jsonb_agg(private.message_snapshot(t.id) order by t.seq desc),'[]') into r from (
 select m.id,m.seq from public.messages m where m.conversation_id=p_conversation and m.seq<p_before and m.media_id is not null and m.deleted_at is null and m.media_kind=p_kind and private.message_visible(m.id) order by m.seq desc limit 30) t;
 return r;
end $$;
create function public.shared_media(p_conversation uuid,p_before bigint default 9223372036854775807,p_kind text default 'image') returns jsonb language sql security invoker set search_path='' as $$select private.shared_media(p_conversation,p_before,p_kind)$$;
revoke all on function private.shared_media(uuid,bigint,text),public.shared_media(uuid,bigint,text) from public,anon;
grant execute on function private.shared_media(uuid,bigint,text),public.shared_media(uuid,bigint,text) to authenticated;

-- A tombstoned attachment stops issuing new URLs immediately and is queued for bounded cleanup.
create function private.retire_deleted_media() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if old.media_id is not null and new.media_id is null and new.deleted_at is not null then
 update private.images set published=false,ready=false where id=old.media_id;
 end if;
 return new;
end $$;
revoke all on function private.retire_deleted_media() from public,anon,authenticated;
create trigger retire_deleted_media after update of media_id on public.messages for each row execute function private.retire_deleted_media();

create or replace function private.notification_targets(p_message uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare o private.notification_outbox;m public.messages;result jsonb;begin
 select * into o from private.notification_outbox where message_id=p_message and state='processing';
 if o.message_id is null then return null;end if;
 select * into m from public.messages where id=p_message;
 if m.deleted_at is not null or exists(select 1 from private.message_preferences pref where pref.user_id=o.recipient_id and pref.message_id=m.id and pref.hidden) or exists(select 1 from public.blocks b where (b.actor=m.sender_id and b.target=o.recipient_id) or (b.target=m.sender_id and b.actor=o.recipient_id)) or exists(select 1 from public.conversation_state s where s.conversation_id=m.conversation_id and s.user_id=o.recipient_id and (s.muted or s.deleted_seq>=m.seq)) then return null;end if;
 select jsonb_build_object('tokens',(select coalesce(jsonb_agg(token),'[]') from public.device_tokens where user_id=o.recipient_id),
 'data',jsonb_build_object('conversationId',m.conversation_id::text,'messageId',m.id::text,'senderId',m.sender_id::text,'recipientId',o.recipient_id::text,
 'title',case when s.notification_previews then p.display_name else 'Liquid Chat' end,'body',case when s.notification_previews then left(m.body,180) else 'New message' end)) into result
 from public.profiles p join public.settings s on s.user_id=o.recipient_id where p.id=m.sender_id;
 return result;
end $$;
