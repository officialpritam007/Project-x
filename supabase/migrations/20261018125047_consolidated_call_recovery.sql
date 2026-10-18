-- Durable call summaries contain no SDP, ICE addresses or device tokens.
create table public.call_history (
 id uuid primary key, conversation_id uuid not null references public.conversations(id) on delete cascade,
 caller_id uuid not null references public.profiles(id) on delete cascade,
 callee_id uuid not null references public.profiles(id) on delete cascade,
 kind text not null, state text not null, created_at timestamptz not null,
 answered_at timestamptz, ended_at timestamptz
);
create index call_history_caller_time on public.call_history(caller_id,created_at desc);
create index call_history_callee_time on public.call_history(callee_id,created_at desc);
alter table public.call_history enable row level security;
revoke all on public.call_history from public,anon,authenticated;
grant select on public.call_history to authenticated;
create policy own_call_history on public.call_history for select to authenticated
 using ((select auth.uid()) in(caller_id,callee_id) and private.session_active());
create function private.record_call_history() returns trigger language plpgsql security definer set search_path='' as $$
begin
 insert into public.call_history(id,conversation_id,caller_id,callee_id,kind,state,created_at,answered_at,ended_at)
 values(new.id,new.conversation_id,new.caller_id,new.callee_id,new.kind,new.state,new.created_at,
 case when new.state='accepted' then now() end,case when new.state in('ended','declined') then now() end)
 on conflict(id) do update set state=excluded.state,
 answered_at=coalesce(public.call_history.answered_at,excluded.answered_at),
 ended_at=coalesce(public.call_history.ended_at,excluded.ended_at);
 return new;
end $$;
revoke all on function private.record_call_history() from public,anon,authenticated;
create trigger record_call_history after insert or update of state on public.call_sessions for each row execute function private.record_call_history();
create function private.list_call_history(p_before timestamptz) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();r jsonb;begin
 select coalesce(jsonb_agg(to_jsonb(t) order by t.created_at desc),'[]') into r from (
 select h.*,p.display_name,p.avatar_id,case when h.caller_id=u then h.callee_id else h.caller_id end as peer_id,
 case when h.state='ringing' and not exists(select 1 from public.call_sessions c where c.id=h.id and c.expires_at>now()) then 'missed' else h.state end as outcome
 from public.call_history h join public.profiles p on p.id=case when h.caller_id=u then h.callee_id else h.caller_id end
 where u in(h.caller_id,h.callee_id) and h.created_at<p_before order by h.created_at desc limit 50)t;
 return r;
end $$;
create function public.list_call_history(p_before timestamptz default 'infinity') returns jsonb language sql security invoker set search_path='' as $$select private.list_call_history(p_before)$$;
revoke all on function private.list_call_history(timestamptz),public.list_call_history(timestamptz) from public,anon;
grant execute on function private.list_call_history(timestamptz),public.list_call_history(timestamptz) to authenticated;
create or replace function private.purge_expired_calls() returns integer language plpgsql security definer set search_path='' as $$
declare removed integer;begin
 update public.call_history h set state=case when h.answered_at is null and h.state='ringing' then 'missed' else 'ended' end,
 ended_at=c.expires_at from public.call_sessions c where c.id=h.id and c.expires_at<now() and h.ended_at is null;
 delete from public.call_sessions where expires_at<now()-interval '1 hour';
 get diagnostics removed=row_count;
 delete from public.call_history where created_at<now()-interval '90 days';return removed;
end $$;
-- Device tokens are confined to the private service-only dispatch state.
create table private.call_dispatch (
 call_id uuid primary key references public.call_sessions(id) on delete cascade,
 attempts int not null default 0,lease_until timestamptz,delivered text[] not null default '{}'
);
alter table private.call_dispatch enable row level security;
revoke all on private.call_dispatch from public,anon,authenticated;
create or replace function private.claim_call_push(p_call uuid,p_caller uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare c public.call_sessions;d private.call_dispatch;tokens jsonb;begin
 select * into c from public.call_sessions where id=p_call for update;
 if not found or c.caller_id<>p_caller or c.state<>'ringing' or c.expires_at<=now() or c.push_sent_at is not null
 or exists(select 1 from public.blocks where (actor=c.caller_id and target=c.callee_id) or (target=c.caller_id and actor=c.callee_id)) then return null;end if;
 insert into private.call_dispatch(call_id) values(p_call) on conflict do nothing;
 select * into d from private.call_dispatch where call_id=p_call for update;
 if d.attempts>=3 or d.lease_until>now() then return null;end if;
 update private.call_dispatch set attempts=attempts+1,lease_until=now()+interval '12 seconds' where call_id=p_call;
 select coalesce(jsonb_agg(token),'[]') into tokens from public.device_tokens where user_id=c.callee_id and not(token=any(d.delivered));
 return jsonb_build_object('recipientId',c.callee_id::text,'conversationId',c.conversation_id::text,'callId',c.id::text,'kind',c.kind,'tokens',tokens);
end $$;
create function private.finish_call_push(p_call uuid,p_delivered text[]) returns void language plpgsql security definer set search_path='' as $$
begin
 update private.call_dispatch set delivered=array(select distinct unnest(delivered||coalesce(p_delivered,'{}'))),lease_until=null where call_id=p_call;
 update public.call_sessions c set push_sent_at=now() where c.id=p_call and not exists(
 select 1 from public.device_tokens t join private.call_dispatch d on d.call_id=c.id where t.user_id=c.callee_id and not(t.token=any(d.delivered)));
end $$;
create function public.finish_call_push(p_call uuid,p_delivered text[]) returns void language sql security invoker set search_path='' as $$select private.finish_call_push(p_call,p_delivered)$$;
revoke all on function private.finish_call_push(uuid,text[]),public.finish_call_push(uuid,text[]) from public,anon,authenticated;
grant execute on function private.finish_call_push(uuid,text[]),public.finish_call_push(uuid,text[]) to service_role;
-- A repeated candidate from transport retry is idempotent; avoid an index over unbounded SDP text.
create index call_candidate_lookup on public.call_candidates(call_id,sender_id);

create or replace function private.call_candidate(p_call uuid,p_payload jsonb)
returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();c public.call_sessions;
begin
 if p_payload is null or jsonb_typeof(p_payload)<>'object' or octet_length(p_payload::text)>4096
 or (p_payload->>'sdpMid') is null or (p_payload->>'candidate') is null
 or length(p_payload->>'candidate')>3000 then
   raise exception 'Invalid ICE candidate' using errcode='22023';
 end if;
 select * into c from public.call_sessions where id=p_call for update;
 if not found or u not in(c.caller_id,c.callee_id) or c.expires_at<=now()
    or c.state not in ('ringing','accepted')
    or private.is_blocked(case when u=c.caller_id then c.callee_id else c.caller_id end) then
   raise exception 'Call unavailable' using errcode='42501';
 end if;
 perform private.limit_action('call_candidate',100);
 if not exists(select 1 from public.call_candidates where call_id=p_call and sender_id=u and payload=p_payload) then
 insert into public.call_candidates(call_id,sender_id,payload) values(p_call,u,p_payload);
 end if;
end $$;
