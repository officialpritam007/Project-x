create or replace function private.image_access(p_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;begin
 select * into i from private.images where id=p_id and ready;
 if i.id is null then raise exception 'Unavailable' using errcode='42501';end if;
 if i.owner_id<>u then
 if i.conversation_id is null then
 if not exists(select 1 from public.profiles p where p.id=i.owner_id and p.avatar_id=i.id::text and (exists(select 1 from public.conversations c where u in(c.user_a,c.user_b) and p.id in(c.user_a,c.user_b)) or exists(select 1 from public.contacts where owner_id=u and target_id=p.id))) or private.is_blocked(i.owner_id) then raise exception 'Unavailable' using errcode='42501';end if;
 else
 if private.is_blocked(i.owner_id) or not private.is_member(i.conversation_id) or not exists(select 1 from public.messages m join public.conversation_state s on s.conversation_id=m.conversation_id and s.user_id=u where m.media_id=i.id and m.seq>s.deleted_seq) then raise exception 'Unavailable' using errcode='42501';end if;
 end if;
 end if;
 return jsonb_build_object('public_id',i.public_id,'format',i.format,'width',i.width,'height',i.height);
end $$;
create or replace function private.set_avatar(p_media uuid) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 if p_media is not null and not exists(select 1 from private.images where id=p_media and owner_id=u and conversation_id is null and ready) then raise exception 'Unavailable' using errcode='42501';end if;
 update private.images set published=false where owner_id=u and conversation_id is null and id::text=(select avatar_id from public.profiles where id=u) and id is distinct from p_media;
 update public.profiles set avatar_id=p_media::text where id=u;
 if p_media is not null then update private.images set published=true where id=p_media;end if;
end $$;
alter table private.notification_outbox add column created_at timestamptz not null default now();
select cron.schedule('liquid-notification-dispatch','*/5 * * * *',$cron$
 select net.http_post(url:='https://qsdmspylumxysczzqnqc.supabase.co/functions/v1/notify',headers:=jsonb_build_object('Content-Type','application/json','apikey','sb_publishable_EIfZYiBXr4XR5eTW6vcPxw_qPLsQs78'),body:=jsonb_build_object('cron_secret',private.dispatch_secret())) where exists(select 1 from private.notification_outbox where state='processing' or (state='pending' and attempts<5));
$cron$);
select cron.schedule('liquid-operational-retention','17 3 * * *',$cron$
 delete from cron.job_run_details where end_time<now()-interval '30 days';
 delete from private.notification_outbox where state in('sent','discarded','failed') and created_at<now()-interval '30 days';
 delete from private.reports where resolved_at<now()-interval '90 days';
$cron$);
