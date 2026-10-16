create or replace function private.report_contact(p_target uuid,p_reason text,p_messages uuid[]) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 if u=p_target or cardinality(p_messages)>10 or not exists(select 1 from public.conversations where u in(user_a,user_b) and p_target in(user_a,user_b)) then raise exception 'Unavailable' using errcode='42501';end if;
 if exists(select 1 from unnest(p_messages) evidence(message_id) where not exists(select 1 from public.messages m join public.conversations c on c.id=m.conversation_id where m.id=evidence.message_id and u in(c.user_a,c.user_b) and p_target in(c.user_a,c.user_b))) then raise exception 'Invalid evidence' using errcode='42501';end if;
 perform private.limit_action('report',2);
 insert into private.reports(actor,target,reason,message_ids) values(u,p_target,trim(p_reason),p_messages);
end $$;
