-- One-to-one calls: signal metadata only. Audio/video media NEVER enters Postgres.
-- Additive migration: no existing chats, messages, or media are rewritten.
create table public.call_sessions (
 id uuid primary key default gen_random_uuid(),
 conversation_id uuid not null references public.conversations(id) on delete cascade,
 caller_id uuid not null references public.profiles(id) on delete cascade,
 callee_id uuid not null references public.profiles(id) on delete cascade,
 kind text not null check(kind in ('audio','video')),
 state text not null default 'ringing' check(state in ('ringing','accepted','declined','ended')),
 offer text not null check(length(offer) between 1 and 65536),
 answer text check(answer is null or length(answer) between 1 and 65536),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now(),
 expires_at timestamptz not null default (now()+interval '45 seconds'),
 check (caller_id<>callee_id)
);
create index call_sessions_incoming_idx on public.call_sessions(callee_id,created_at desc) where state='ringing';
create index call_sessions_caller_idx on public.call_sessions(caller_id,created_at desc);
create table public.call_candidates (
 id bigint generated always as identity primary key,
 call_id uuid not null references public.call_sessions(id) on delete cascade,
 sender_id uuid not null references public.profiles(id) on delete cascade,
 payload jsonb not null check(octet_length(payload::text) <= 4096),
 created_at timestamptz not null default now()
);
create index call_candidates_call_idx on public.call_candidates(call_id,id);
alter table public.call_sessions enable row level security;
alter table public.call_candidates enable row level security;
revoke all on public.call_sessions,public.call_candidates from public,anon,authenticated;
grant select on public.call_sessions,public.call_candidates to authenticated;
create policy call_session_participants on public.call_sessions for select to authenticated
 using ((select auth.uid()) in (caller_id,callee_id)
   and not private.is_blocked(case when caller_id=(select auth.uid()) then callee_id else caller_id end));
create policy call_candidate_participants on public.call_candidates for select to authenticated
 using (exists(select 1 from public.call_sessions c where c.id=call_id
   and (select auth.uid()) in(c.caller_id,c.callee_id)
   and not private.is_blocked(case when c.caller_id=(select auth.uid()) then c.callee_id else c.caller_id end)));

create function private.call_start(p_conversation uuid,p_kind text,p_offer text)
returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); c public.conversations; peer uuid; new_id uuid;
begin
 if p_kind not in ('audio','video') or length(p_offer) not between 1 and 65536 then
   raise exception 'Invalid call request' using errcode='22023';
 end if;
 select * into c from public.conversations where id=p_conversation;
 if not found or u not in (c.user_a,c.user_b) then raise exception 'Conversation unavailable' using errcode='42501'; end if;
 peer:=case when u=c.user_a then c.user_b else c.user_a end;
 if private.is_blocked(peer) then raise exception 'Contact unavailable' using errcode='42501'; end if;
 perform private.limit_action('call_start',6);
 -- Serialize both participants to prevent simultaneous calls ringing each other.
 perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(least(u,peer)::text));
 perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(greatest(u,peer)::text));
 if exists(select 1 from public.call_sessions where state in ('ringing','accepted')
    and expires_at>now() and (caller_id in(u,peer) or callee_id in(u,peer))) then
   raise exception 'One of you is already in a call' using errcode='P0001';
 end if;
 insert into public.call_sessions(conversation_id,caller_id,callee_id,kind,offer)
 values(p_conversation,u,peer,p_kind,p_offer) returning id into new_id;
 return new_id;
end $$;
create function public.call_start(p_conversation uuid,p_kind text,p_offer text)
returns uuid language sql security invoker set search_path='' as $$
 select private.call_start(p_conversation,p_kind,p_offer)
$$;

create function private.call_respond(p_call uuid,p_accept boolean,p_answer text default null)
returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); c public.call_sessions;
begin
 select * into c from public.call_sessions where id=p_call for update;
 if not found or c.callee_id<>u or private.is_blocked(c.caller_id)
    or c.state<>'ringing' or c.expires_at<=now() then
   raise exception 'Call no longer available' using errcode='42501';
 end if;
 if p_accept and (p_answer is null or length(p_answer) not between 1 and 65536) then
   raise exception 'Invalid answer' using errcode='22023';
 end if;
 update public.call_sessions set state=case when p_accept then 'accepted' else 'declined' end,
   answer=case when p_accept then p_answer else null end,
   updated_at=now(),expires_at=case when p_accept then now()+interval '45 seconds' else now() end
 where id=p_call;
end $$;
create function public.call_respond(p_call uuid,p_accept boolean,p_answer text default null)
returns void language sql security invoker set search_path='' as $$
 select private.call_respond(p_call,p_accept,p_answer)
$$;

create function private.call_hangup(p_call uuid) returns void
language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();
begin
 update public.call_sessions set state='ended',updated_at=now(),expires_at=now()
 where id=p_call and u in(caller_id,callee_id) and state in ('ringing','accepted');
end $$;
create function public.call_hangup(p_call uuid) returns void
language sql security invoker set search_path='' as $$select private.call_hangup(p_call)$$;

create function private.call_heartbeat(p_call uuid) returns void
language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); c public.call_sessions;
begin
 select * into c from public.call_sessions where id=p_call for update;
 if not found or u not in(c.caller_id,c.callee_id) or c.state not in ('ringing','accepted')
  or c.expires_at<=now() or private.is_blocked(case when u=c.caller_id then c.callee_id else c.caller_id end) then
    raise exception 'Call unavailable' using errcode='42501';
 end if;
 -- Ringing stays bounded to its original 45s: only accepted calls renew.
 if c.state='accepted' then
   update public.call_sessions set expires_at=now()+interval '45 seconds',updated_at=now() where id=p_call;
 end if;
end $$;
create function public.call_heartbeat(p_call uuid) returns void
language sql security invoker set search_path='' as $$select private.call_heartbeat(p_call)$$;

create function private.call_candidate(p_call uuid,p_payload jsonb)
returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();c public.call_sessions;
begin
 if p_payload is null or jsonb_typeof(p_payload)<>'object' or octet_length(p_payload::text)>4096
 or (p_payload->>'sdpMid') is null or (p_payload->>'candidate') is null
 or length(p_payload->>'candidate')>3000 then
   raise exception 'Invalid ICE candidate' using errcode='22023';
 end if;
 select * into c from public.call_sessions where id=p_call;
 if not found or u not in(c.caller_id,c.callee_id) or c.expires_at<=now()
    or c.state not in ('ringing','accepted')
    or private.is_blocked(case when u=c.caller_id then c.callee_id else c.caller_id end) then
   raise exception 'Call unavailable' using errcode='42501';
 end if;
 perform private.limit_action('call_candidate',100);
 insert into public.call_candidates(call_id,sender_id,payload) values(p_call,u,p_payload);
end $$;
create function public.call_candidate(p_call uuid,p_payload jsonb)
returns void language sql security invoker set search_path='' as $$
select private.call_candidate(p_call,p_payload)
$$;

create function private.call_poll(p_call uuid default null,p_after bigint default 0)
returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=private.require_user(); c public.call_sessions; latest jsonb; ice jsonb:='[]'::jsonb;
begin
 if p_call is null then
   select * into c from public.call_sessions
   where callee_id=u and state='ringing' and expires_at>now()
     and not private.is_blocked(caller_id)
   order by created_at desc limit 1;
 else
   select * into c from public.call_sessions
   where id=p_call and u in(caller_id,callee_id);
   if found and private.is_blocked(case when u=c.caller_id then c.callee_id else c.caller_id end) then
     raise exception 'Contact unavailable' using errcode='42501';
   end if;
 end if;
 if not found then return jsonb_build_object('call',null,'signals',ice); end if;
 if p_call is not null then
  select coalesce(jsonb_agg(to_jsonb(s) order by s.id),'[]'::jsonb) into ice
  from (select id,sender_id,payload from public.call_candidates
        where call_id=p_call and id>greatest(0,p_after)
        order by id limit 100) s;
 end if;
 latest:=to_jsonb(c);
 return jsonb_build_object('call',latest,'signals',ice);
end $$;
create function public.call_poll(p_call uuid default null,p_after bigint default 0)
returns jsonb language sql stable security invoker set search_path='' as $$
select private.call_poll(p_call,p_after)
$$;

revoke all on function private.call_start(uuid,text,text),private.call_respond(uuid,boolean,text),
 private.call_hangup(uuid),private.call_heartbeat(uuid),private.call_candidate(uuid,jsonb),
 private.call_poll(uuid,bigint) from public,anon,authenticated;
revoke all on function public.call_start(uuid,text,text),public.call_respond(uuid,boolean,text),
 public.call_hangup(uuid),public.call_heartbeat(uuid),public.call_candidate(uuid,jsonb),
 public.call_poll(uuid,bigint) from public,anon;
grant execute on function public.call_start(uuid,text,text),public.call_respond(uuid,boolean,text),
 public.call_hangup(uuid),public.call_heartbeat(uuid),public.call_candidate(uuid,jsonb),
 public.call_poll(uuid,bigint) to authenticated;

-- SDP and ICE candidates can contain network identifiers; enforce short retention.
create function private.purge_expired_calls() returns integer
language plpgsql security definer set search_path='' as $$
declare removed integer;
begin
 delete from public.call_sessions
 where expires_at < now()-interval '1 hour';
 get diagnostics removed = row_count;
 return removed;
end $$;
revoke all on function private.purge_expired_calls() from public,anon,authenticated;
-- Existing project already uses pg_cron. This job deletes call metadata, not chat messages.
select cron.schedule('liquid-call-retention','*/15 * * * *',
 'select private.purge_expired_calls()');
-- Deploy only after rollback-only authorization fixtures pass.
