create or replace function private.deletion_batch(p_job uuid) returns jsonb language sql security definer set search_path='' as $$
 select jsonb_build_object('id',j.id,'user_id',j.user_id,'auth_exists',exists(select 1 from auth.users where id=j.user_id),'images',(select coalesce(jsonb_agg(row_to_json(i)),'[]') from (select id,public_id from private.images where owner_id=j.user_id and not destroyed limit 20)i)) from private.deletion_jobs j where j.state='pending' and (p_job is null or j.id=p_job) order by j.requested_at limit 1;
$$;
