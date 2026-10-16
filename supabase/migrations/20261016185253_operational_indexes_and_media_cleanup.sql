create or replace function private.list_chats(p_before_seq bigint,p_before_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); result jsonb;
begin
 select coalesce(jsonb_agg(row_to_json(t)::jsonb order by t.last_seq desc,t.id desc),'[]') into result from (
 select c.id,c.last_seq,c.user_a,c.user_b,p.username,p.display_name,p.avatar_id,
 s.favorite,s.archived,s.muted,s.deleted_seq,s.read_seq,
 peer.delivered_seq as peer_delivered_seq,peer.read_seq as peer_read_seq,
 (select count(*) from public.messages m where m.conversation_id=c.id and m.sender_id<>u and m.seq>greatest(s.read_seq,s.deleted_seq)) as unread,
 (select m.created_at from public.messages m where m.conversation_id=c.id and m.seq>s.deleted_seq order by m.seq desc limit 1) as last_at,
 (select m.body from public.messages m where m.conversation_id=c.id and m.seq>s.deleted_seq order by m.seq desc limit 1) as preview
 from public.conversations c
 join public.conversation_state s on s.conversation_id=c.id and s.user_id=u
 join public.profiles p on p.id=case when c.user_a=u then c.user_b else c.user_a end
 join public.conversation_state peer on peer.conversation_id=c.id and peer.user_id=p.id
 where u in(c.user_a,c.user_b) and (c.last_seq,c.id)<(p_before_seq,p_before_id)
 and (c.last_seq>s.deleted_seq or c.last_seq=0)
 order by c.last_seq desc,c.id desc limit 100
 ) t;
 return result;
end $$;

create or replace function private.is_blocked(peer uuid) returns boolean language sql stable security definer set search_path='' as $$
 select auth.uid() is null or not exists(select 1 from public.profiles where id=peer and not is_deleted) or exists(select 1 from public.blocks b where (b.actor=auth.uid() and b.target=peer) or (b.target=auth.uid() and b.actor=peer));
$$;
create index typing_user_idx on private.typing(user_id);
create index notification_recipient_idx on private.notification_outbox(recipient_id);
create or replace function private.claim_notifications(p_sender uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;begin
 update private.notification_outbox set state='failed',last_error='LEASE_EXHAUSTED' where state='processing' and lease_until<now() and attempts>=5;
 with candidates as (
 select o.message_id from private.notification_outbox o join public.messages m on m.id=o.message_id
 where (p_sender is null or m.sender_id=p_sender) and o.attempts<5 and ((o.state='pending' and o.available_at<=now()) or (o.state='processing' and o.lease_until<now()))
 order by o.available_at for update of o skip locked limit 20
 ), claimed as (
 update private.notification_outbox o set state='processing',attempts=attempts+1,lease_until=now()+interval '90 seconds' from candidates c where o.message_id=c.message_id returning o.message_id
 ) select coalesce(jsonb_agg(message_id),'[]') into r from claimed;
 return r;
end $$;

create function private.garbage_images() returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;begin
 with candidates as (select id from private.images where not published and not destroyed and created_at<now()-interval '1 hour' order by created_at for update skip locked limit 20), claimed as (update private.images i set ready=false from candidates c where i.id=c.id and not i.published returning i.id,i.public_id) select coalesce(jsonb_agg(row_to_json(claimed)),'[]') into r from claimed;return r;
end $$;
create function public.garbage_images() returns jsonb language sql security invoker set search_path='' as $$select private.garbage_images()$$;
create function private.finish_garbage(p_id uuid) returns void language plpgsql security definer set search_path='' as $$begin
 delete from private.images where id=p_id and not published and not ready and not exists(select 1 from public.messages where media_id=p_id) and not exists(select 1 from public.profiles where avatar_id=p_id::text);
end $$;
create function public.finish_garbage(p_id uuid) returns void language sql security invoker set search_path='' as $$select private.finish_garbage(p_id)$$;
revoke all on function private.garbage_images(),public.garbage_images(),private.finish_garbage(uuid),public.finish_garbage(uuid) from public,anon,authenticated;
grant execute on function private.garbage_images(),public.garbage_images(),private.finish_garbage(uuid),public.finish_garbage(uuid) to service_role;
select cron.schedule('liquid-media-cleanup','*/30 * * * *',$cron$
 select net.http_post(url:='https://qsdmspylumxysczzqnqc.supabase.co/functions/v1/media-cleanup',headers:=jsonb_build_object('Content-Type','application/json','apikey','sb_publishable_EIfZYiBXr4XR5eTW6vcPxw_qPLsQs78'),body:=jsonb_build_object('cron_secret',private.dispatch_secret())) where exists(select 1 from private.images where not published and not destroyed and created_at<now()-interval '1 hour');
$cron$);
