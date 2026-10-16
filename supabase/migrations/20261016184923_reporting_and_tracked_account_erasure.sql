-- Preserve anonymized sender IDs for the other participant's existing text history.
-- This removes a constraint, not users or messages; account erasure is a separate authenticated job.
alter table public.profiles drop constraint profiles_id_fkey;
alter table public.profiles add column is_deleted boolean not null default false;
alter table private.images add column destroyed boolean not null default false;
create table private.deletion_jobs(id uuid primary key default gen_random_uuid(),user_id uuid not null unique,requested_at timestamptz not null default now(),state text not null default 'pending');
alter table private.deletion_jobs enable row level security;
create policy deletion_internal on private.deletion_jobs for all to authenticated using(false) with check(false);
create table private.reports(id uuid primary key default gen_random_uuid(),actor uuid not null,target uuid not null,reason text not null check(char_length(reason) between 1 and 1000),message_ids uuid[] not null default '{}',created_at timestamptz not null default now(),resolved_at timestamptz);
alter table private.reports enable row level security;
create policy reports_internal on private.reports for all to authenticated using(false) with check(false);
create function private.report_contact(p_target uuid,p_reason text,p_messages uuid[]) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 if u=p_target or cardinality(p_messages)>10 or not exists(select 1 from public.conversations where u in(user_a,user_b) and p_target in(user_a,user_b)) then raise exception 'Unavailable' using errcode='42501';end if;
 if exists(select 1 from unnest(p_messages) id where not exists(select 1 from public.messages m join public.conversations c on c.id=m.conversation_id where m.id=id and u in(c.user_a,c.user_b) and p_target in(c.user_a,c.user_b))) then raise exception 'Invalid evidence' using errcode='42501';end if;
 perform private.limit_action('report',2);
 insert into private.reports(actor,target,reason,message_ids) values(u,p_target,trim(p_reason),p_messages);
end $$;
create function public.report_contact(p_target uuid,p_reason text,p_messages uuid[] default '{}') returns void language sql security invoker set search_path='' as $$select private.report_contact(p_target,p_reason,p_messages)$$;
create function private.begin_deletion() returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();job uuid;begin
 if not exists(select 1 from auth.sessions where id=(auth.jwt()->>'session_id')::uuid and user_id=u and created_at>now()-interval '5 minutes') then raise exception 'Sign in again before deleting your account' using errcode='42501';end if;
 insert into private.deletion_jobs(user_id) values(u) on conflict(user_id) do update set state='pending' returning id into job;
 loop
 begin
 update public.profiles set username='deleted_'||left(replace(gen_random_uuid()::text,'-',''),16),display_name='Deleted account',bio='',avatar_id=null,is_deleted=true where id=u;
 exit;
 exception when unique_violation then null;
 end;
 end loop;
 update public.settings set show_presence=false,read_receipts=false,notification_previews=false where user_id=u;
 update private.images set ready=false where owner_id=u;
 delete from public.device_tokens where user_id=u;
 return job;
end $$;
create function public.begin_deletion() returns uuid language sql security invoker set search_path='' as $$select private.begin_deletion()$$;
create function private.deletion_batch(p_job uuid) returns jsonb language sql security definer set search_path='' as $$
 select jsonb_build_object('id',j.id,'user_id',j.user_id,'images',(select coalesce(jsonb_agg(row_to_json(i)),'[]') from (select id,public_id from private.images where owner_id=j.user_id and not destroyed limit 20)i)) from private.deletion_jobs j where j.state='pending' and (p_job is null or j.id=p_job) order by j.requested_at limit 1;
$$;
create function public.deletion_batch(p_job uuid default null) returns jsonb language sql security invoker set search_path='' as $$select private.deletion_batch(p_job)$$;
create function private.mark_image_destroyed(p_id uuid) returns void language sql security definer set search_path='' as $$update private.images set destroyed=true,ready=false where id=p_id$$;
create function public.mark_image_destroyed(p_id uuid) returns void language sql security invoker set search_path='' as $$select private.mark_image_destroyed(p_id)$$;
create function private.finish_deletion(p_job uuid) returns boolean language plpgsql security definer set search_path='' as $$
declare u uuid;begin
 select user_id into u from private.deletion_jobs where id=p_job for update;
 if u is null or exists(select 1 from private.images where owner_id=u and not destroyed) or exists(select 1 from auth.users where id=u) then return false;end if;
 delete from public.device_tokens where user_id=u;
 delete from private.presence where user_id=u;
 delete from private.typing where user_id=u;
 delete from public.blocks where actor=u or target=u;
 update public.conversation_state set favorite=false,archived=false,muted=true where user_id=u;
 update private.notification_outbox o set state='discarded' where o.recipient_id=u or exists(select 1 from public.messages m where m.id=o.message_id and m.sender_id=u);
 update private.deletion_jobs set state='done' where id=p_job;
 return true;
end $$;
create function public.finish_deletion(p_job uuid) returns boolean language sql security invoker set search_path='' as $$select private.finish_deletion(p_job)$$;
create or replace function private.session_active() returns boolean language sql stable security definer set search_path='' as $$
 select auth.uid() is not null and exists(select 1 from auth.sessions s where s.id=nullif(auth.jwt()->>'session_id','')::uuid and s.user_id=auth.uid() and (s.not_after is null or s.not_after>now())) and not exists(select 1 from public.profiles p where p.id=auth.uid() and p.is_deleted);
$$;
create or replace function private.find_profile(p_username text) returns setof public.profiles language plpgsql security definer set search_path='' as $$
begin perform private.limit_action('discovery',20);return query select p.* from public.profiles p where p.username=lower(trim(p_username)) and not p.is_deleted and not private.is_blocked(p.id) limit 1;end $$;
revoke all on function private.report_contact(uuid,text,uuid[]),public.report_contact(uuid,text,uuid[]),private.begin_deletion(),public.begin_deletion() from public,anon;
grant execute on function private.report_contact(uuid,text,uuid[]),public.report_contact(uuid,text,uuid[]),private.begin_deletion(),public.begin_deletion() to authenticated;
revoke all on function private.deletion_batch(uuid),private.mark_image_destroyed(uuid),private.finish_deletion(uuid),public.deletion_batch(uuid),public.mark_image_destroyed(uuid),public.finish_deletion(uuid) from public,anon,authenticated;
grant execute on function private.deletion_batch(uuid),private.mark_image_destroyed(uuid),private.finish_deletion(uuid),public.deletion_batch(uuid),public.mark_image_destroyed(uuid),public.finish_deletion(uuid) to service_role;
select cron.schedule('liquid-account-cleanup','*/5 * * * *',$cron$
 select net.http_post(url:='https://qsdmspylumxysczzqnqc.supabase.co/functions/v1/account',headers:=jsonb_build_object('Content-Type','application/json','apikey','sb_publishable_EIfZYiBXr4XR5eTW6vcPxw_qPLsQs78'),body:=jsonb_build_object('cron_secret',private.dispatch_secret())) where exists(select 1 from private.deletion_jobs where state='pending');
$cron$);
