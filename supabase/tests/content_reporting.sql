-- Rollback-only isolated moderation fixtures; never use real accounts.
begin;
insert into auth.users(id,email) values
 ('a1000000-0000-0000-0000-000000000001','report-owner@example.invalid'),
 ('a1000000-0000-0000-0000-000000000002','report-viewer@example.invalid'),
 ('a1000000-0000-0000-0000-000000000003','report-stranger@example.invalid');
insert into auth.sessions(id,user_id,created_at) select id,id,now() from auth.users where id in
 ('a1000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000003');
insert into public.profiles(id,username,display_name) values
 ('a1000000-0000-0000-0000-000000000001','report_owner','Owner'),
 ('a1000000-0000-0000-0000-000000000002','report_viewer','Viewer'),
 ('a1000000-0000-0000-0000-000000000003','report_stranger','Stranger');
insert into public.contacts(owner_id,target_id) values('a1000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000002');
insert into private.images(id,owner_id,public_id,format,width,height,bytes,ready,published) values
 ('a2000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000001','isolated-report-story','jpg',1,1,10,true,true),
 ('a2000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000001','isolated-report-post','jpg',1,1,10,true,true);
insert into private.moments(id,owner_id,media_id,kind,expires_at) values
 ('a3000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000001','a2000000-0000-0000-0000-000000000001','story',now()+interval '1 day'),
 ('a3000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000001','a2000000-0000-0000-0000-000000000002','post',null);
do $$begin if has_function_privilege('anon','public.report_moment(uuid,text)','execute') then raise exception 'FAIL: anonymous report grant';end if;end$$;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"a1000000-0000-0000-0000-000000000003","role":"authenticated","session_id":"a1000000-0000-0000-0000-000000000003"}',true);
select set_config('request.jwt.claim.sub','a1000000-0000-0000-0000-000000000003',true);
do $$begin begin perform public.report_moment('a3000000-0000-0000-0000-000000000001','unrelated');raise exception 'FAIL: stranger report';exception when insufficient_privilege then null;end;end$$;
select set_config('request.jwt.claims','{"sub":"a1000000-0000-0000-0000-000000000001","role":"authenticated","session_id":"a1000000-0000-0000-0000-000000000001"}',true);
select set_config('request.jwt.claim.sub','a1000000-0000-0000-0000-000000000001',true);
do $$begin begin perform public.report_moment('a3000000-0000-0000-0000-000000000001','own');raise exception 'FAIL: self report';exception when insufficient_privilege then null;end;end$$;
select set_config('request.jwt.claims','{"sub":"a1000000-0000-0000-0000-000000000002","role":"authenticated","session_id":"a1000000-0000-0000-0000-000000000002"}',true);
select set_config('request.jwt.claim.sub','a1000000-0000-0000-0000-000000000002',true);
do $$begin begin perform public.report_moment('a3000000-0000-0000-0000-000000000001',' ');raise exception 'FAIL: empty reason';exception when invalid_parameter_value then null;end;end$$;
select public.report_moment('a3000000-0000-0000-0000-000000000001','Fixture story reason');
select public.report_moment('a3000000-0000-0000-0000-000000000002','Fixture post reason');
do $$begin begin perform public.report_moment('a3000000-0000-0000-0000-000000000002','third');raise exception 'FAIL: report rate limit';exception when raise_exception then if sqlerrm not like '%Please wait%' then raise;end if;end;end$$;
select public.set_block('a1000000-0000-0000-0000-000000000001',true);
do $$begin begin perform public.report_moment('a3000000-0000-0000-0000-000000000001','blocked');raise exception 'FAIL: blocked report';exception when insufficient_privilege then null;end;end$$;
select public.set_block('a1000000-0000-0000-0000-000000000001',false);
reset role;
update private.moments set archived=true where id='a3000000-0000-0000-0000-000000000002';
update private.moments set expires_at=now()-interval '1 second' where id='a3000000-0000-0000-0000-000000000001';
set local role authenticated;
do $$begin
 begin perform public.report_moment('a3000000-0000-0000-0000-000000000001','expired');raise exception 'FAIL: expired';exception when insufficient_privilege then null;end;
 begin perform public.report_moment('a3000000-0000-0000-0000-000000000002','archived');raise exception 'FAIL: archived';exception when insufficient_privilege then null;end;
end$$;
reset role;
do $$begin
 if (select count(*) from private.reports where actor='a1000000-0000-0000-0000-000000000002')<>2 then raise exception 'FAIL: report count';end if;
 if not exists(select 1 from private.reports where actor='a1000000-0000-0000-0000-000000000002' and reason like '[story:a3000000-0000-0000-0000-000000000001]%') then raise exception 'FAIL: unbound evidence';end if;
end$$;
-- Revoked sessions cannot submit even for currently visible content.
update private.moments set archived=false,expires_at=case when kind='story' then now()+interval '1 day' else null end where owner_id='a1000000-0000-0000-0000-000000000001';
delete from auth.sessions where id='a1000000-0000-0000-0000-000000000002';
set local role authenticated;
do $$begin begin perform public.report_moment('a3000000-0000-0000-0000-000000000001','revoked session');raise exception 'FAIL: revoked report session';exception when insufficient_privilege then null;end;end$$;
reset role;
select 'PASS: private content reports, actor/evidence binding, anonymous/stranger/self/block/expiry/archive denial and shared rate limit' as result;
rollback;
