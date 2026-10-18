-- One-to-one call authorization regression. Run against migrated schema as DB owner.
-- All synthetic identities, calls, ICE candidates and state are rolled back.
begin;
insert into auth.users(id,email) values
 ('91000000-0000-0000-0000-000000000001','call-a@example.invalid'),
 ('91000000-0000-0000-0000-000000000002','call-b@example.invalid'),
 ('91000000-0000-0000-0000-000000000003','call-c@example.invalid');
insert into auth.sessions(id,user_id,created_at) select id,id,now()
 from auth.users where id::text like '91000000-%' on conflict do nothing;
set local role authenticated;
select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"91000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"91000000-0000-0000-0000-000000000001"}',true);
select public.save_profile('call_test_a','Caller','');
select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"91000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"91000000-0000-0000-0000-000000000002"}',true);
select public.save_profile('call_test_b','Recipient','');
select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"91000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"91000000-0000-0000-0000-000000000003"}',true);
select public.save_profile('call_test_c','Outsider','');
select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"91000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"91000000-0000-0000-0000-000000000001"}',true);
select set_config('liquid.call.cid', public.start_conversation('91000000-0000-0000-0000-000000000002')::text,true);
select set_config('liquid.call.id',
 public.call_start(current_setting('liquid.call.cid')::uuid,'audio','synthetic-sdp-offer')::text,true);
do $$declare id uuid:=current_setting('liquid.call.id')::uuid; data jsonb;begin
 data:=public.call_poll(id,0);
 if data->'call'->>'caller_id'<>'91000000-0000-0000-0000-000000000001' then
   raise exception 'FAIL: caller cannot read their call';
 end if;
 begin
   perform public.call_start(current_setting('liquid.call.cid')::uuid,'video','second-offer');
   raise exception 'FAIL: double call accepted';
 exception when raise_exception then
   if SQLERRM like 'FAIL:%' then raise; end if;
 end;
 begin
   insert into public.call_candidates(call_id,sender_id,payload)
    values(id,'91000000-0000-0000-0000-000000000001','{}'::jsonb);
   raise exception 'FAIL: direct candidate write';
 exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"91000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"91000000-0000-0000-0000-000000000003"}',true);
do $$declare id uuid:=current_setting('liquid.call.id')::uuid;begin
 if (select count(*) from public.call_sessions where call_sessions.id=current_setting('liquid.call.id')::uuid)<>0 then
   raise exception 'FAIL: outsider read call';
 end if;
 if public.call_poll(id,0)->'call' <> 'null'::jsonb then raise exception 'FAIL: outsider poll exposed call';end if;
 begin
   perform public.call_candidate(id,'{"sdpMid":"0","candidate":"synthetic"}'::jsonb);
   raise exception 'FAIL: outsider candidate';
 exception when insufficient_privilege then null; end;
 begin
   perform public.call_respond(id,true,'malicious-answer');
   raise exception 'FAIL: outsider answer';
 exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"91000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"91000000-0000-0000-0000-000000000002"}',true);
do $$declare id uuid:=current_setting('liquid.call.id')::uuid;begin
 if public.call_poll(null,0)->'call'->>'id' <> id::text then
   raise exception 'FAIL: incoming call not discoverable';
 end if;
 perform public.call_respond(id,true,'synthetic-sdp-answer');
 if public.call_poll(id,0)->'call'->>'state'<>'accepted' then
   raise exception 'FAIL: answer not visible';
 end if;
 perform public.call_candidate(id,'{"sdpMid":"0","sdpMLineIndex":0,"candidate":"synthetic-ice"}'::jsonb);
 if jsonb_array_length(public.call_poll(id,0)->'signals')<>1 then
   raise exception 'FAIL: candidate not visible';
 end if;
end $$;
select set_config('request.jwt.claim.sub','91000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"91000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"91000000-0000-0000-0000-000000000001"}',true);
do $$declare id uuid:=current_setting('liquid.call.id')::uuid;begin
 if jsonb_array_length(public.call_poll(id,0)->'signals')<>1 then
   raise exception 'FAIL: candidate invisible to peer';
 end if;
 perform public.call_hangup(id);
 if public.call_poll(id,0)->'call'->>'state'<>'ended' then
   raise exception 'FAIL: end not reflected';
 end if;
end $$;
select public.set_block('91000000-0000-0000-0000-000000000002',true);
do $$begin
 begin
  perform public.call_start(current_setting('liquid.call.cid')::uuid,'video','offer');
  raise exception 'FAIL: block did not prevent calling';
 exception when insufficient_privilege then null; end;
end $$;

-- A one-shot invitation may only be claimed by service_role, never a signed-in user.
select public.set_block('91000000-0000-0000-0000-000000000002',false);
select set_config('liquid.call.pushid',public.call_start(current_setting('liquid.call.cid')::uuid,'video','push-offer')::text,true);
do $$begin
  begin
    perform public.claim_call_push(current_setting('liquid.call.pushid')::uuid,
      '91000000-0000-0000-0000-000000000001');
    raise exception 'FAIL: user claimed FCM push';
  exception when insufficient_privilege then null; end;
end $$;
reset role;
set local role service_role;
do $$declare payload jsonb; begin
 payload:=public.claim_call_push(current_setting('liquid.call.pushid')::uuid,
   '91000000-0000-0000-0000-000000000001');
 if payload->>'recipientId'<>'91000000-0000-0000-0000-000000000002' then
   raise exception 'FAIL: wrong push recipient'; end if;
 if public.claim_call_push(current_setting('liquid.call.pushid')::uuid,
   '91000000-0000-0000-0000-000000000001') is not null then
   raise exception 'FAIL: push claimed twice'; end if;
end $$;
reset role;
rollback;
