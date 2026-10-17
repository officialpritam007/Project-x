create policy message_preferences_rpc_only on private.message_preferences for all to authenticated using(false) with check(false);
create index message_preferences_message_idx on private.message_preferences(message_id);
create function private.starred_messages(p_conversation uuid,p_before bigint) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user();r jsonb;begin
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 select coalesce(jsonb_agg(private.message_snapshot(t.id) order by t.seq desc),'[]') into r from (
 select m.id,m.seq from private.message_preferences p join public.messages m on m.id=p.message_id
 where p.user_id=u and p.starred and not p.hidden and m.conversation_id=p_conversation and m.seq<p_before and m.deleted_at is null and private.message_visible(m.id)
 order by m.seq desc limit 30) t;
 return r;
end $$;
create function public.starred_messages(p_conversation uuid,p_before bigint default 9223372036854775807) returns jsonb language sql security invoker set search_path='' as $$select private.starred_messages(p_conversation,p_before)$$;
revoke all on function private.starred_messages(uuid,bigint),public.starred_messages(uuid,bigint) from public,anon;
grant execute on function private.starred_messages(uuid,bigint),public.starred_messages(uuid,bigint) to authenticated;
create or replace function private.list_chats(p_before_seq bigint,p_before_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); result jsonb;
begin
 select coalesce(jsonb_agg(row_to_json(t)::jsonb order by t.last_seq desc,t.id desc),'[]') into result from (
 select c.id,c.last_seq,c.user_a,c.user_b,p.username,p.display_name,p.avatar_id,
 s.favorite,s.archived,s.muted,s.deleted_seq,s.read_seq,s.pinned_at,s.manual_unread,
 peer.delivered_seq as peer_delivered_seq,case when coalesce((select read_receipts from public.settings where user_id=p.id),false) then peer.read_seq else 0 end as peer_read_seq,
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
