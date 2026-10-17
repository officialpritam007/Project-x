-- Keep private preference activity out of the peer-visible message/conversation streams.
alter table private.message_preferences add column change_seq bigint not null default 0;
create index message_preferences_changes_idx on private.message_preferences(user_id,change_seq) where change_seq>0;
create or replace function private.message_snapshot(p_id uuid) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user();m public.messages;begin
 select * into m from public.messages where id=p_id;
 if m.id is null or not private.is_member(m.conversation_id) then raise exception 'Message unavailable' using errcode='42501';end if;
 if not private.message_visible(p_id) then return jsonb_build_object('id',m.id,'hidden',true,'change_seq',greatest(m.change_seq,coalesce((select p.change_seq from private.message_preferences p where p.user_id=u and p.message_id=m.id),0)));end if;
 return to_jsonb(m)||jsonb_build_object('change_seq',greatest(m.change_seq,coalesce((select p.change_seq from private.message_preferences p where p.user_id=u and p.message_id=m.id),0)),'starred',coalesce((select starred from private.message_preferences p where p.user_id=u and p.message_id=m.id),false));
end $$;
create or replace function private.message_changes(p_conversation uuid,p_after bigint) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user();result jsonb;begin
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 with candidates as (
   select m.id,m.change_seq from public.messages m where m.conversation_id=p_conversation and m.change_seq>greatest(p_after,0)
   union all
   select m.id,p.change_seq from private.message_preferences p join public.messages m on m.id=p.message_id
    where p.user_id=u and m.conversation_id=p_conversation and p.change_seq>greatest(p_after,0)
 ), page as (select id,max(change_seq) as change_seq from candidates group by id order by max(change_seq) limit 100)
 select coalesce(jsonb_agg(private.message_snapshot(t.id) order by t.change_seq),'[]') into result from page t;
 return result;
end $$;
create or replace function private.message_action(p_id uuid,p_action text,p_value text) returns jsonb language plpgsql security definer set search_path='' as $$
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
 if p_action in('star','hide') then
  update private.message_preferences set change_seq=nextval('private.message_change_seq') where user_id=u and message_id=p_id;
  -- This row's RLS is owner-only. No conversation/message update reveals private activity.
  update public.conversation_state set manual_unread=manual_unread where conversation_id=c.id and user_id=u;
 else
  update public.messages set change_seq=nextval('private.message_change_seq') where id=p_id;
  update public.conversations set last_seq=last_seq where id=c.id;
 end if;
 return private.message_snapshot(p_id);
end $$;
