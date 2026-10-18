CREATE OR REPLACE FUNCTION private.finish_notification(p_message uuid, p_ok boolean, p_error text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
 update private.notification_outbox set state=case when p_ok then 'sent' when attempts>=5 then 'failed' else 'pending' end,
 available_at=now()+make_interval(secs => least(1800,(30*power(2,attempts))::int)),lease_until=null,last_error=left(p_error,80) where message_id=p_message and state='processing';
end $function$;

CREATE OR REPLACE FUNCTION private.notification_preview(p_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare u uuid:=private.require_user();r jsonb;begin
 select jsonb_build_object('conversation_id',m.conversation_id,'title',case when s.notification_previews then p.display_name else 'Liquid Chat' end,
 'body',case when s.notification_previews then left(m.body,180) else 'New message' end,'previews',s.notification_previews) into r
 from public.messages m join public.profiles p on p.id=m.sender_id join public.settings s on s.user_id=u
 join public.conversation_state cs on cs.user_id=u and cs.conversation_id=m.conversation_id
 where m.id=p_id and m.sender_id<>u and m.deleted_at is null and private.message_visible(m.id) and m.seq>greatest(cs.read_seq,cs.deleted_seq) and not cs.muted and not private.is_blocked(m.sender_id);
 return r;
end $function$;

CREATE OR REPLACE FUNCTION private.notification_targets(p_message uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare o private.notification_outbox;m public.messages;result jsonb;begin
 select * into o from private.notification_outbox where message_id=p_message and state='processing';
 if o.message_id is null then return null;end if;
 select * into m from public.messages where id=p_message;
 if m.deleted_at is not null or exists(select 1 from private.message_preferences pref where pref.user_id=o.recipient_id and pref.message_id=m.id and pref.hidden) or exists(select 1 from public.blocks b where (b.actor=m.sender_id and b.target=o.recipient_id) or (b.target=m.sender_id and b.actor=o.recipient_id)) or exists(select 1 from public.conversation_state s where s.conversation_id=m.conversation_id and s.user_id=o.recipient_id and (s.muted or greatest(s.deleted_seq,s.read_seq)>=m.seq)) then return null;end if;
 select jsonb_build_object('tokens',(select coalesce(jsonb_agg(token),'[]') from public.device_tokens where user_id=o.recipient_id),
 'data',jsonb_build_object('conversationId',m.conversation_id::text,'messageId',m.id::text,'senderId',m.sender_id::text,'recipientId',o.recipient_id::text,
 'title',case when s.notification_previews then p.display_name else 'Liquid Chat' end,'body',case when s.notification_previews then left(m.body,180) else 'New message' end)) into result
 from public.profiles p join public.settings s on s.user_id=o.recipient_id where p.id=m.sender_id;
 return result;
end $function$;
