-- Rollback-only integration tests. No real users, uploads or notifications.
begin;
insert into auth.users(id,email) values('90000000-0000-0000-0000-000000000001','moments-test-1@example.invalid');
insert into auth.sessions(id,user_id,created_at) values('90000000-0000-0000-0000-000000000001','90000000-0000-0000-0000-000000000001',now());
insert into auth.users(id,email) values('90000000-0000-0000-0000-000000000002','moments-test-2@example.invalid');
insert into auth.sessions(id,user_id,created_at) values('90000000-0000-0000-0000-000000000002','90000000-0000-0000-0000-000000000002',now());
insert into auth.users(id,email) values('90000000-0000-0000-0000-000000000003','moments-test-3@example.invalid');
insert into auth.sessions(id,user_id,created_at) values('90000000-0000-0000-0000-000000000003','90000000-0000-0000-0000-000000000003',now());
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);
select public.save_profile('moments_test_1','Moment Test 1','');
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000002"}',true);
select public.save_profile('moments_test_2','Moment Test 2','');
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000003"}',true);
select public.save_profile('moments_test_3','Moment Test 3','');
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);
select public.save_contact('90000000-0000-0000-0000-000000000002',true);
select set_config('test.image',(public.prepare_image(null)->>'id'),true);
reset role;
select public.finalize_image(current_setting('test.image')::uuid,'jpg',100,120,1234);
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);

select public.publish_moment('91000000-0000-0000-0000-000000000001',current_setting('test.image')::uuid,'story','test caption');
select public.publish_moment('91000000-0000-0000-0000-000000000001',current_setting('test.image')::uuid,'story','test caption');
do $$begin if jsonb_array_length(public.list_moments('story',true))<>1 then raise exception 'FAIL: idempotent story';end if;end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000002"}',true);

do $$begin if jsonb_array_length(public.list_moments('story'))<>1 then raise exception 'FAIL: contact feed';end if;end$$;
select public.image_access(current_setting('test.image')::uuid);
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000003"}',true);
select public.save_contact('90000000-0000-0000-0000-000000000001',true);

do $$begin
 if jsonb_array_length(public.list_moments('story'))<>0 then raise exception 'FAIL: reverse contact audience';end if;
 begin perform public.image_access(current_setting('test.image')::uuid);raise exception 'FAIL: outsider image';exception when insufficient_privilege then null;end;
 begin perform public.change_moment('91000000-0000-0000-0000-000000000001','delete');raise exception 'FAIL: outsider delete';exception when insufficient_privilege then null;end;
 begin perform public.publish_moment(gen_random_uuid(),current_setting('test.image')::uuid,'post','steal');raise exception 'FAIL: stolen media';exception when insufficient_privilege then null;end;
 begin perform 1 from private.moments;raise exception 'FAIL: direct private read';exception when insufficient_privilege then null;end;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);
select set_config('test.cid',public.start_conversation('90000000-0000-0000-0000-000000000002')::text,true);

select set_config('test.voice',(public.prepare_voice(current_setting('test.cid')::uuid)->>'id'),true);
reset role;
select public.finalize_voice(current_setting('test.voice')::uuid,'m4a',8000,64000);
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);

select public.send_voice('92000000-0000-0000-0000-000000000001',current_setting('test.voice')::uuid);
select public.send_voice('92000000-0000-0000-0000-000000000001',current_setting('test.voice')::uuid);
do $$begin
 if (select count(*) from public.messages where conversation_id=current_setting('test.cid')::uuid)<>1 then raise exception 'FAIL: duplicate voice';end if;
 if not exists(select 1 from public.messages where id='92000000-0000-0000-0000-000000000001' and media_kind='voice' and duration_ms=8000) then raise exception 'FAIL: voice metadata';end if;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000003',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000003"}',true);

do $$begin
 begin perform public.prepare_voice(current_setting('test.cid')::uuid);raise exception 'FAIL: outsider voice intent';exception when insufficient_privilege then null;end;
 begin perform public.image_access(current_setting('test.voice')::uuid);raise exception 'FAIL: outsider playback';exception when insufficient_privilege then null;end;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000002"}',true);
select public.image_access(current_setting('test.voice')::uuid);
select public.set_block('90000000-0000-0000-0000-000000000001',true);

do $$begin
 if jsonb_array_length(public.list_moments('story'))<>0 then raise exception 'FAIL: blocked feed';end if;
 begin perform public.image_access(current_setting('test.voice')::uuid);raise exception 'FAIL: blocked playback';exception when insufficient_privilege then null;end;
 begin perform public.image_access(current_setting('test.image')::uuid);raise exception 'FAIL: blocked story';exception when insufficient_privilege then null;end;
end$$;
select public.set_block('90000000-0000-0000-0000-000000000001',false);
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);
select set_config('test.postimage',(public.prepare_image(null)->>'id'),true);
reset role;
select public.finalize_image(current_setting('test.postimage')::uuid,'jpg',100,100,1234);
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);

select public.publish_moment('91000000-0000-0000-0000-000000000002',current_setting('test.postimage')::uuid,'post','post');
select public.change_moment('91000000-0000-0000-0000-000000000002','archive');
do $$begin
 if jsonb_array_length(public.list_moments('post',true,false))<>0 or jsonb_array_length(public.list_moments('post',true,true))<>1 then raise exception 'FAIL: archive';end if;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000002"}',true);

do $$begin
 if jsonb_array_length(public.list_moments('post'))<>0 then raise exception 'FAIL: archived feed';end if;
 begin perform public.image_access(current_setting('test.postimage')::uuid);raise exception 'FAIL: archived media exposed';exception when insufficient_privilege then null;end;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);

select public.change_moment('91000000-0000-0000-0000-000000000002','restore');
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000002"}',true);

do $$begin if jsonb_array_length(public.list_moments('post'))<>1 then raise exception 'FAIL: restore';end if;end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);
select public.save_contact('90000000-0000-0000-0000-000000000002',false);
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000002"}',true);

do $$begin
 if jsonb_array_length(public.list_moments('post'))<>0 then raise exception 'FAIL: audience revocation';end if;
 begin perform public.image_access(current_setting('test.postimage')::uuid);raise exception 'FAIL: revoked media';exception when insufficient_privilege then null;end;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);
select public.save_contact('90000000-0000-0000-0000-000000000002',true);
reset role;
update private.moments set expires_at=now()-interval '1 second' where kind='story' and owner_id='90000000-0000-0000-0000-000000000001';
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000002"}',true);

do $$begin
 if jsonb_array_length(public.list_moments('story'))<>0 then raise exception 'FAIL: expired story feed';end if;
 begin perform public.image_access(current_setting('test.image')::uuid);raise exception 'FAIL: expired media';exception when insufficient_privilege then null;end;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000001"}',true);

select public.change_moment('91000000-0000-0000-0000-000000000002','delete');
do $$begin if jsonb_array_length(public.list_moments('post',true))<>0 then raise exception 'FAIL: delete';end if;end$$;
reset role;
set local role authenticated;
select set_config('test.pending',(public.prepare_upload(null,'image','supabase')->>'id'),true);
reset role;
do $$begin if not exists(select 1 from private.images where id=current_setting('test.pending')::uuid and provider='supabase' and format='jpg' and not ready) then raise exception 'FAIL: atomic provider/format';end if;end$$;
insert into private.images(id,owner_id,public_id,kind,format,width,height,bytes,ready,provider) values('93000000-0000-0000-0000-000000000001','90000000-0000-0000-0000-000000000002','quota-test-global','image','jpg',1,1,536870912,true,'supabase');
do $$begin
 begin perform public.finalize_image(current_setting('test.pending')::uuid,'jpg',1,1,100);raise exception 'FAIL: global quota';exception when raise_exception then if sqlerrm not like '%Shared media budget%' then raise;end if;end;
end$$;
delete from private.images where id='93000000-0000-0000-0000-000000000001';
insert into private.images(id,owner_id,public_id,kind,format,width,height,bytes,ready) values('93000000-0000-0000-0000-000000000002','90000000-0000-0000-0000-000000000001','quota-test-owner','image','jpg',1,1,104857600,true);
do $$begin
 begin perform public.finalize_image(current_setting('test.pending')::uuid,'jpg',1,1,100);raise exception 'FAIL: owner quota';exception when raise_exception then if sqlerrm not like '%Media limit%' then raise;end if;end;
end$$;
set local role authenticated;
select public.message_action('92000000-0000-0000-0000-000000000001','delete');
do $$begin
 begin perform public.image_access(current_setting('test.voice')::uuid);raise exception 'FAIL: deleted owner voice access';exception when insufficient_privilege then null;end;
end$$;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claims','{"sub":"90000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"90000000-0000-0000-0000-000000000002"}',true);
do $$begin
 begin perform public.image_access(current_setting('test.voice')::uuid);raise exception 'FAIL: deleted recipient voice access';exception when insufficient_privilege then null;end;
 if jsonb_array_length(public.shared_media(current_setting('test.cid')::uuid,9223372036854775807,'voice'))<>0 then raise exception 'FAIL: deleted voice browsing';end if;
end$$;
reset role;
select 'PASS: contact audience, reverse-contact denial, ownership, block, expiry, archive/restore/delete, private media and idempotent participant-only voice' as result;
rollback;
