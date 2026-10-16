create function private.list_chats(p_before_seq bigint,p_before_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); result jsonb;
begin
 select coalesce(jsonb_agg(row_to_json(t)::jsonb order by t.last_seq desc,t.id desc),'[]') into result from (
 select c.id,c.last_seq,c.user_a,c.user_b,p.username,p.display_name,p.avatar_id,
 s.favorite,s.archived,s.muted,s.deleted_seq,s.read_seq,
 peer.delivered_seq as peer_delivered_seq,peer.read_seq as peer_read_seq,
 (select count(*) from public.messages m where m.conversation_id=c.id and m.sender_id<>u and m.seq>greatest(s.read_seq,s.deleted_seq)) as unread,
 (select m.body from public.messages m where m.conversation_id=c.id and m.seq>s.deleted_seq order by m.seq desc limit 1) as preview
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
create function public.list_chats(p_before_seq bigint default 9223372036854775807,p_before_id uuid default 'ffffffff-ffff-ffff-ffff-ffffffffffff') returns jsonb language sql security invoker set search_path='' as $$select private.list_chats(p_before_seq,p_before_id)$$;
revoke all on function private.list_chats(bigint,uuid) from public,anon;
grant execute on function private.list_chats(bigint,uuid) to authenticated;
revoke all on function public.list_chats(bigint,uuid) from public,anon;
grant execute on function public.list_chats(bigint,uuid) to authenticated;
-- Receipt hints notify both participants without exposing the recipient's settings.
create function private.receipt_hint() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.delivered_seq<>old.delivered_seq or new.read_seq<>old.read_seq then
 update public.conversations set last_seq=last_seq where id=new.conversation_id;
 end if;
 return new;
end $$;
revoke all on function private.receipt_hint() from public,anon,authenticated;
create trigger receipt_hint after update of delivered_seq,read_seq on public.conversation_state for each row execute function private.receipt_hint();
alter publication supabase_realtime add table public.conversations,public.messages,public.conversation_state;
