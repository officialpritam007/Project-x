-- Run as the database owner. Fixtures and all effects are rolled back.
begin;
insert into auth.users(id,email) values
 ('10000000-0000-0000-0000-000000000001','liquid-test-a@example.invalid'),
 ('10000000-0000-0000-0000-000000000002','liquid-test-b@example.invalid'),
 ('10000000-0000-0000-0000-000000000003','liquid-test-c@example.invalid');
insert into auth.sessions(id,user_id,created_at) select id,id,now() from auth.users where id in('10000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000003') on conflict do nothing;
set local role authenticated;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000001"}',true);
select public.save_profile('liquid_test_a','Test A','');
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000002"}',true);
select public.save_profile('liquid_test_b','Test B','');
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000003"}',true);
select public.save_profile('liquid_test_c','Test C','');
do $$begin
 begin
  perform public.save_profile('LIQUID_TEST_A','Duplicate','');
  raise exception 'FAIL: duplicate username accepted';
 exception when unique_violation then null; end;
end$$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000001"}',true);
select set_config('liquid.test.cid',public.start_conversation('10000000-0000-0000-0000-000000000002')::text,true);
do $$declare c uuid:=current_setting('liquid.test.cid')::uuid; s bigint; r bigint;begin
 if c<>public.start_conversation('10000000-0000-0000-0000-000000000002') then raise exception 'FAIL: duplicate conversation'; end if;
 select seq into s from public.send_text('20000000-0000-0000-0000-000000000001',c,'hello');
 select seq into r from public.send_text('20000000-0000-0000-0000-000000000001',c,'hello');
 if s<>r or (select count(*) from public.messages where conversation_id=c)<>1 then raise exception 'FAIL: duplicate message'; end if;
 begin
  insert into public.messages(id,conversation_id,sender_id,body) values(gen_random_uuid(),c,'10000000-0000-0000-0000-000000000002','spoof');
  raise exception 'FAIL: direct message write';
 exception when insufficient_privilege then null; end;
end$$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000003"}',true);
do $$begin
 if exists(select 1 from public.messages) or exists(select 1 from public.conversations) then raise exception 'FAIL: nonparticipant read'; end if;
 if (select count(*) from public.profiles)<>1 then raise exception 'FAIL: directory exposure'; end if;
 begin
  perform public.send_text(gen_random_uuid(),current_setting('liquid.test.cid')::uuid,'intruder');
  raise exception 'FAIL: nonparticipant send';
 exception when insufficient_privilege then null; end;
end$$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000002"}',true);
select public.save_contact('10000000-0000-0000-0000-000000000001',true);
do $$begin if jsonb_array_length(public.list_contacts())<>1 then raise exception 'FAIL: save contact';end if;end$$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000001"}',true);
do $$begin if jsonb_array_length(public.list_contacts())<>0 then raise exception 'FAIL: contact ownership';end if;end$$;
select set_config('liquid.test.avatar',public.prepare_image(null)->>'id',true);
reset role;
select public.finalize_image(current_setting('liquid.test.avatar')::uuid,'jpg',100,100,1234);
set local role authenticated;
select public.set_avatar(current_setting('liquid.test.avatar')::uuid);
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000002"}',true);
select public.image_access(current_setting('liquid.test.avatar')::uuid);
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000001"}',true);
select public.set_avatar(null);
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000002"}',true);
do $$begin begin perform public.image_access(current_setting('liquid.test.avatar')::uuid);raise exception 'FAIL: removed avatar remains accessible';exception when insufficient_privilege then null;end;end$$;
select public.save_settings(false,true,true);
select public.update_presence(true);
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000001"}',true);
do $$declare state jsonb;begin
 state:=public.contact_state(current_setting('liquid.test.cid')::uuid);
 if (state->>'online')::boolean or state->>'last_seen' is not null then raise exception 'FAIL: hidden presence';end if;
 begin
  perform 1 from private.presence;
  raise exception 'FAIL: heartbeat exposure';
 exception when insufficient_privilege then null;end;
end$$;
select set_config('liquid.test.image',(public.prepare_image(current_setting('liquid.test.cid')::uuid)->>'id'),true);
reset role;
select public.finalize_image(current_setting('liquid.test.image')::uuid,'jpg',100,100,1234);
set local role authenticated;
select public.send_image('20000000-0000-0000-0000-000000000002',current_setting('liquid.test.image')::uuid,'Photo');
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000002"}',true);
select public.image_access(current_setting('liquid.test.image')::uuid);
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000003"}',true);
do $$begin
 begin
  perform public.image_access(current_setting('liquid.test.image')::uuid);
  raise exception 'FAIL: nonparticipant image access';
 exception when insufficient_privilege then null;end;
end$$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000002"}',true);
select public.set_block('10000000-0000-0000-0000-000000000001',true);
do $$begin
 begin
  perform public.send_text(gen_random_uuid(),current_setting('liquid.test.cid')::uuid,'blocked outgoing');
  raise exception 'FAIL: blocked outgoing';
 exception when insufficient_privilege then null; end;
end$$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000001"}',true);
do $$begin
 begin
  perform public.send_text(gen_random_uuid(),current_setting('liquid.test.cid')::uuid,'blocked incoming');
  raise exception 'FAIL: blocked incoming';
 exception when insufficient_privilege then null; end;
end$$;
select public.set_conversation(current_setting('liquid.test.cid')::uuid,false,false,false,true);
do $$begin
 if exists(select 1 from public.messages) then raise exception 'FAIL: deletion watermark bypass'; end if;
end$$;
select public.report_contact('10000000-0000-0000-0000-000000000002','Fixture report, no evidence attached');
do $$begin
 begin perform 1 from private.reports;raise exception 'FAIL: report queue exposure';exception when insufficient_privilege then null;end;
 begin perform public.claim_notifications(null);raise exception 'FAIL: notification administration exposed';exception when insufficient_privilege then null;end;
end$$;
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000003"}',true);
select set_config('liquid.test.deletion',public.begin_deletion()::text,true);
do $$begin if exists(select 1 from public.profiles) then raise exception 'FAIL: deletion did not disable access';end if;end$$;
reset role;
delete from auth.users where id='10000000-0000-0000-0000-000000000003';
select public.finish_deletion(current_setting('liquid.test.deletion')::uuid);
do $$begin if not exists(select 1 from public.profiles where id='10000000-0000-0000-0000-000000000003' and is_deleted and display_name='Deleted account' and bio='' and avatar_id is null) then raise exception 'FAIL: profile erasure';end if;end$$;
update auth.sessions set created_at=now()-interval '10 minutes' where id='10000000-0000-0000-0000-000000000001';
select set_config('request.jwt.claim.sub','10000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"10000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"10000000-0000-0000-0000-000000000001"}',true);
set local role authenticated;
do $$begin begin perform public.begin_deletion();raise exception 'FAIL: deletion accepted without recent reauth';exception when insufficient_privilege then null;end;end$$;
reset role;
delete from auth.sessions where id='10000000-0000-0000-0000-000000000001';
set local role authenticated;
do $$begin
 if exists(select 1 from public.profiles) or exists(select 1 from public.conversations) then raise exception 'FAIL: revoked session read';end if;
 begin perform public.save_profile('revoked_user','Revoked','');raise exception 'FAIL: revoked session write';exception when insufficient_privilege then null;end;
end$$;
reset role;
select 'PASS: uniqueness, pair identity, idempotency, spoof denial, nonparticipant isolation, both-way blocks, deletion watermark, hidden presence, private image access, revoked-session denial, restricted reports/dispatcher, reauthenticated account erasure, contact isolation, removed-avatar denial' as result;
rollback;
