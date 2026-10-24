-- Additive protocol v2. Existing call/session/block authorization remains authoritative.
alter table public.call_sessions
 add column presented boolean not null default false,
 add column caller_media jsonb not null default '{"camera":true,"held":false}',
 add column callee_media jsonb not null default '{"camera":true,"held":false}',
 add column revision integer not null default 0,
 add column restart_request integer not null default 0,
 add column restart_offer text not null default '',
 add column restart_answer text not null default '';

create function private.call_update(p_call uuid,p_payload jsonb) returns void
language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();c public.call_sessions;a text:=p_payload->>'action';m jsonb;sequence bigint;
begin
 select * into c from public.call_sessions where id=p_call for update;
 if not found or u not in(c.caller_id,c.callee_id) or c.expires_at<=now()
 or c.state not in('ringing','accepted')
 or private.is_blocked(case when u=c.caller_id then c.callee_id else c.caller_id end) then
 raise exception 'Call unavailable' using errcode='42501';end if;
 if p_payload is null or octet_length(p_payload::text)>70000 then
 raise exception 'Invalid call update' using errcode='22023';end if;
 perform private.limit_action('call_update',60);
 if a='presented' and u=c.callee_id and c.state='ringing' then
  update public.call_sessions set presented=true where id=p_call;
 elsif a='media' then
  if jsonb_typeof(p_payload->'camera')<>'boolean' or jsonb_typeof(p_payload->'held')<>'boolean'
    or not(p_payload ? 'camera' and p_payload ? 'held') then raise exception 'Invalid media state' using errcode='22023';end if;
  sequence:=(p_payload->>'sequence')::bigint;
  if sequence is null or sequence<0 or sequence>2147483647 then raise exception 'Invalid media sequence' using errcode='22023';end if;
  if sequence<=coalesce(((case when u=c.caller_id then c.caller_media else c.callee_media end)->>'sequence')::bigint,-1) then return;end if;
  m:=jsonb_build_object('camera',p_payload->'camera','held',p_payload->'held','sequence',sequence);
  update public.call_sessions set caller_media=case when u=c.caller_id then m else caller_media end,
   callee_media=case when u=c.callee_id then m else callee_media end where id=p_call;
 elsif a='restart' and c.state='accepted' then
  if c.restart_request>=6 then raise exception 'Restart limit reached' using errcode='22023';end if;
  update public.call_sessions set restart_request=restart_request+1 where id=p_call;
 elsif a='offer' and u=c.caller_id and c.state='accepted' and (c.revision=0 or c.restart_answer<>'') then
  if length(p_payload->>'sdp') not between 1 and 65536 or p_payload->>'sdp' is null then raise exception 'Invalid SDP' using errcode='22023';end if;
  update public.call_sessions set restart_offer=p_payload->>'sdp',restart_answer='',revision=revision+1 where id=p_call;
 elsif a='answer' and u=c.callee_id and c.state='accepted' and (p_payload->>'revision')::integer=c.revision then
  if length(p_payload->>'sdp') not between 1 and 65536 or p_payload->>'sdp' is null then raise exception 'Invalid SDP' using errcode='22023';end if;
  update public.call_sessions set restart_answer=p_payload->>'sdp' where id=p_call;
 else raise exception 'Invalid call update' using errcode='22023';end if;
end $$;
create function public.call_update(p_call uuid,p_payload jsonb) returns void
language sql security invoker set search_path='' as $$select private.call_update(p_call,p_payload)$$;
revoke all on function private.call_update(uuid,jsonb),public.call_update(uuid,jsonb) from public,anon;
grant execute on function private.call_update(uuid,jsonb),public.call_update(uuid,jsonb) to authenticated;

-- Authenticated TURN issuance authorizes the conversation without returning its data.
create function private.call_ice_authorize(p_conversation uuid) returns boolean
language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();c public.conversations;peer uuid;
begin
 select * into c from public.conversations where id=p_conversation;
 if not found or u not in(c.user_a,c.user_b) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 peer:=case when u=c.user_a then c.user_b else c.user_a end;
 if private.is_blocked(peer) then raise exception 'Contact unavailable' using errcode='42501';end if;
 perform private.limit_action('call_ice',12);return true;
end $$;
create function public.call_ice_authorize(p_conversation uuid) returns boolean
language sql security invoker set search_path='' as $$select private.call_ice_authorize(p_conversation)$$;
revoke all on function private.call_ice_authorize(uuid),public.call_ice_authorize(uuid) from public,anon;
grant execute on function private.call_ice_authorize(uuid),public.call_ice_authorize(uuid) to authenticated;

alter table private.call_dispatch add column claim_id uuid, add column tried jsonb not null default '{}';

-- Claim every pending device. Edge dispatch uses concurrency-limited batches, never a slice.
-- Lease spans the entire bounded dispatch deadline, so concurrent retries cannot overlap.
create or replace function private.claim_call_push(p_call uuid,p_caller uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare c public.call_sessions;d private.call_dispatch;tokens jsonb;begin
 select * into c from public.call_sessions where id=p_call for update;
 if not found or c.caller_id<>p_caller or c.state<>'ringing' or c.expires_at<=now() or c.push_sent_at is not null
 or exists(select 1 from public.blocks where (actor=c.caller_id and target=c.callee_id) or (target=c.caller_id and actor=c.callee_id)) then return null;end if;
 insert into private.call_dispatch(call_id) values(p_call) on conflict do nothing;
 select * into d from private.call_dispatch where call_id=p_call for update;
 if d.attempts>=3 or d.lease_until>now() then return null;end if;
 update private.call_dispatch set attempts=attempts+1,claim_id=gen_random_uuid(),lease_until=now()+interval '35 seconds' where call_id=p_call returning * into d;
 select coalesce(jsonb_agg(token order by coalesce((d.tried->>token)::int,0),token),'[]') into tokens from public.device_tokens where user_id=c.callee_id and not(token=any(d.delivered));
 return jsonb_build_object('claimId',d.claim_id::text,'expiresAt',c.expires_at,'recipientId',c.callee_id::text,'conversationId',c.conversation_id::text,'callId',c.id::text,'kind',c.kind,'tokens',tokens);
end $$;

-- Completion is tied to the claim; a delayed worker cannot clear a newer lease.
create function private.finish_call_push_v2(p_call uuid,p_claim uuid,p_attempted text[],p_delivered text[]) returns boolean
language plpgsql security definer set search_path='' as $$
declare d private.call_dispatch;t text;complete boolean;
begin
 -- Match claim's call-session -> dispatch lock order, including concurrent retries.
 perform 1 from public.call_sessions where id=p_call for update;
 if not found then return false;end if;
 select * into d from private.call_dispatch where call_id=p_call for update;
 if not found or d.claim_id is distinct from p_claim then return false;end if;
 foreach t in array coalesce(p_attempted,'{}') loop
  d.tried:=jsonb_set(d.tried,array[t],to_jsonb(coalesce((d.tried->>t)::integer,0)+1));
 end loop;
 update private.call_dispatch set tried=d.tried,
 delivered=array(select distinct unnest(delivered||coalesce(p_delivered,'{}'))),lease_until=null
 where call_id=p_call;
 select not exists(select 1 from public.device_tokens t join public.call_sessions c on c.id=p_call
 join private.call_dispatch dispatch on dispatch.call_id=c.id where t.user_id=c.callee_id and not(t.token=any(dispatch.delivered))) into complete;
 if complete then update public.call_sessions set push_sent_at=now() where id=p_call;end if;
 return complete;
end $$;
create function public.finish_call_push_v2(p_call uuid,p_claim uuid,p_attempted text[],p_delivered text[]) returns boolean
language sql security invoker set search_path='' as $$select private.finish_call_push_v2(p_call,p_claim,p_attempted,p_delivered)$$;
revoke all on function private.finish_call_push_v2(uuid,uuid,text[],text[]),public.finish_call_push_v2(uuid,uuid,text[],text[]) from public,anon,authenticated;
grant execute on function private.finish_call_push_v2(uuid,uuid,text[],text[]),public.finish_call_push_v2(uuid,uuid,text[],text[]) to service_role;
