-- Optional, authenticated FCM wakeup for already-authorized 1:1 call invitations.
-- Additive only. No credentials in this schema; service_role is the sole claimant.
alter table public.call_sessions add column push_sent_at timestamptz;

create function private.claim_call_push(p_call uuid,p_caller uuid) returns jsonb
language plpgsql security definer set search_path='' as $$
declare c public.call_sessions; tokens jsonb;
begin
 select * into c from public.call_sessions where id=p_call for update;
 if not found or c.caller_id<>p_caller or c.state<>'ringing'
   or c.expires_at<=now() or c.push_sent_at is not null
   or exists (select 1 from public.blocks b
     where (b.actor=c.caller_id and b.target=c.callee_id)
        or (b.actor=c.callee_id and b.target=c.caller_id)) then
   return null;
 end if;
 update public.call_sessions set push_sent_at=now() where id=c.id;
 select coalesce(jsonb_agg(t.token),'[]'::jsonb) into tokens
 from public.device_tokens t where t.user_id=c.callee_id;
 return jsonb_build_object(
   'recipientId',c.callee_id::text,'conversationId',c.conversation_id::text,
   'callId',c.id::text,'kind',c.kind,'tokens',tokens
 );
end $$;
create function public.claim_call_push(p_call uuid,p_caller uuid) returns jsonb
language sql security invoker set search_path='' as $$
 select private.claim_call_push(p_call,p_caller)
$$;
revoke all on function private.claim_call_push(uuid,uuid),public.claim_call_push(uuid,uuid)
 from public,anon,authenticated;
grant execute on function private.claim_call_push(uuid,uuid),public.claim_call_push(uuid,uuid)
 to service_role;
