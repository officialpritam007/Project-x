-- Run as the database owner. Fixtures and all effects are rolled back.
begin;
insert into auth.users(id,email) values
 ('30000000-0000-0000-0000-000000000001','modern-test-a@example.invalid'),
 ('30000000-0000-0000-0000-000000000002','modern-test-b@example.invalid'),
 ('30000000-0000-0000-0000-000000000003','modern-test-c@example.invalid');
insert into auth.sessions(id,user_id,created_at) select id,id,now() from auth.users where id in('30000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000002','30000000-0000-0000-0000-000000000003') on conflict do nothing;
set local role authenticated;
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000001"}',true);
select public.save_profile('modern_test_a','Test A','');
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000002"}',true);
select public.save_profile('modern_test_b','Test B','');
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000003"}',true);
select public.save_profile('modern_test_c','Test C','');
do $$begin
 begin
  perform public.save_profile('MODERN_TEST_A','Duplicate','');
  raise exception 'FAIL: duplicate username accepted';
 exception when unique_violation then null; end;
end$$;
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000001"}',true);
select set_config('liquid.test.cid',public.start_conversation('30000000-0000-0000-0000-000000000002')::text,true);

-- Reply is same-conversation and retry-idempotent; reactions are one per actor.
select public.send_text('40000000-0000-0000-0000-000000000001',current_setting('liquid.test.cid')::uuid,'Original');
select public.send_reply('40000000-0000-0000-0000-000000000002',current_setting('liquid.test.cid')::uuid,'Reply','40000000-0000-0000-0000-000000000001');
select public.send_reply('40000000-0000-0000-0000-000000000002',current_setting('liquid.test.cid')::uuid,'Reply','40000000-0000-0000-0000-000000000001');
select public.message_action('40000000-0000-0000-0000-000000000001','react','👍');
select public.message_action('40000000-0000-0000-0000-000000000001','star','true');
do $$declare j jsonb;begin
 if (select count(*) from public.messages)<>2 then raise exception 'FAIL duplicate reply';end if;
 j:=public.message_by_id('40000000-0000-0000-0000-000000000001');
 if j->>'starred'<>'true' or j->'reactions'->>'30000000-0000-0000-0000-000000000001'<>'👍' then raise exception 'FAIL action persistence';end if;
 begin perform public.message_action('40000000-0000-0000-0000-000000000001','react','not emoji');raise exception 'FAIL reaction allowlist';exception when invalid_parameter_value then null;end;
 begin update public.messages set body='Spoof';raise exception 'FAIL direct edit allowed';exception when insufficient_privilege then null;end;
end$$;
select public.message_action('40000000-0000-0000-0000-000000000001','edit','Edited original');
select public.chat_action(current_setting('liquid.test.cid')::uuid,'pin',true);
select public.chat_action(current_setting('liquid.test.cid')::uuid,'unread',true);
do $$declare j jsonb;begin
 j:=public.message_changes(current_setting('liquid.test.cid')::uuid,0);
 if jsonb_array_length(j)<>1 or j->0->>'body'<>'Edited original' then raise exception 'FAIL changed-message catch-up';end if;
 j:=public.list_chats();
 if j->0->>'pinned_at' is null or j->0->>'manual_unread'<>'true' then raise exception 'FAIL chat preference';end if;
end$$;
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000002"}',true);
do $$begin
 if (public.message_by_id('40000000-0000-0000-0000-000000000001')->>'starred')::boolean then raise exception 'FAIL shared star';end if;
 begin perform public.message_action('40000000-0000-0000-0000-000000000001','edit','Unauthorized');raise exception 'FAIL non-owner edit';exception when insufficient_privilege then null;end;
 begin perform public.message_action('40000000-0000-0000-0000-000000000001','delete');raise exception 'FAIL non-owner delete';exception when insufficient_privilege then null;end;
 if public.notification_preview('40000000-0000-0000-0000-000000000001')->>'body'<>'Edited original' then raise exception 'FAIL fresh preview';end if;
end$$;
select public.message_action('40000000-0000-0000-0000-000000000001','hide');
do $$begin
 if exists(select 1 from public.messages where id='40000000-0000-0000-0000-000000000001') then raise exception 'FAIL hidden message resurfaces';end if;
 if public.message_by_id('40000000-0000-0000-0000-000000000001')->>'hidden'<>'true' then raise exception 'FAIL hidden tombstone';end if;
 if public.notification_preview('40000000-0000-0000-0000-000000000001') is not null then raise exception 'FAIL hidden notification';end if;
end$$;
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000003"}',true);
do $$begin
 begin perform public.message_action('40000000-0000-0000-0000-000000000001','react','👍');raise exception 'FAIL outsider reaction';exception when insufficient_privilege then null;end;
 begin perform public.message_changes(current_setting('liquid.test.cid')::uuid,0);raise exception 'FAIL outsider change feed';exception when insufficient_privilege then null;end;
 begin perform public.message_by_id('40000000-0000-0000-0000-000000000001');raise exception 'FAIL outsider snapshot';exception when insufficient_privilege then null;end;
 begin perform public.chat_action(current_setting('liquid.test.cid')::uuid,'pin',true);raise exception 'FAIL outsider pin';exception when insufficient_privilege then null;end;
end$$;
select set_config('liquid.test.other',public.start_conversation('30000000-0000-0000-0000-000000000001')::text,true);
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000001"}',true);
do $$begin
 begin perform public.send_reply(gen_random_uuid(),current_setting('liquid.test.other')::uuid,'Cross chat','40000000-0000-0000-0000-000000000001');raise exception 'FAIL cross-chat reply';exception when insufficient_privilege then null;end;
end$$;
select public.set_block('30000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000002"}',true);
do $$begin
 begin perform public.message_action('40000000-0000-0000-0000-000000000002','react','👍');raise exception 'FAIL blocked reaction';exception when insufficient_privilege then null;end;
 begin perform public.send_reply(gen_random_uuid(),current_setting('liquid.test.cid')::uuid,'Blocked','40000000-0000-0000-0000-000000000002');raise exception 'FAIL blocked reply';exception when insufficient_privilege then null;end;
 if public.notification_preview('40000000-0000-0000-0000-000000000002') is not null then raise exception 'FAIL blocked notification';end if;
end$$;
select set_config('request.jwt.claim.sub','30000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"30000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"30000000-0000-0000-0000-000000000001"}',true);
select public.set_block('30000000-0000-0000-0000-000000000002',false);
select public.message_action('40000000-0000-0000-0000-000000000002','delete');
do $$declare j jsonb;begin
 j:=public.message_by_id('40000000-0000-0000-0000-000000000002');
 if j->>'deleted_at' is null or j->>'body'<>'This message was deleted' or j->'reactions'<>'{}'::jsonb then raise exception 'FAIL delete tombstone';end if;
 begin perform public.send_reply(gen_random_uuid(),current_setting('liquid.test.cid')::uuid,'Deleted','40000000-0000-0000-0000-000000000002');raise exception 'FAIL reply to deleted';exception when insufficient_privilege then null;end;
end$$;
-- Expired windows are checked by the server, independently of UI timestamps.
reset role;
update public.messages set created_at=now()-interval '49 hours' where id='40000000-0000-0000-0000-000000000001';
set local role authenticated;
do $$begin
 begin perform public.message_action('40000000-0000-0000-0000-000000000001','edit','Too late');raise exception 'FAIL edit window';exception when insufficient_privilege then null;end;
 begin perform public.message_action('40000000-0000-0000-0000-000000000001','delete');raise exception 'FAIL delete window';exception when insufficient_privilege then null;end;
end$$;
reset role;
rollback;
