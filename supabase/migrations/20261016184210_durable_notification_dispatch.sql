create table public.device_tokens(token text primary key check(char_length(token) between 30 and 4096),user_id uuid not null references public.profiles(id),updated_at timestamptz not null default now());
create index device_tokens_user_idx on public.device_tokens(user_id);
alter table public.device_tokens enable row level security;
revoke all on public.device_tokens from anon,authenticated;
grant select on public.device_tokens to authenticated;
create policy device_owner on public.device_tokens for select to authenticated using(user_id=(select auth.uid()));
create table private.notification_outbox(message_id uuid primary key references public.messages(id) on delete cascade,recipient_id uuid not null references public.profiles(id),state text not null default 'pending',attempts int not null default 0,available_at timestamptz not null default now(),lease_until timestamptz,last_error text);
alter table private.notification_outbox enable row level security;
create policy notifications_internal on private.notification_outbox for all to authenticated using(false) with check(false);
create index notification_pending_idx on private.notification_outbox(state,available_at);
create function private.queue_notification() returns trigger language plpgsql security definer set search_path='' as $$
begin
 insert into private.notification_outbox(message_id,recipient_id) select new.id,case when c.user_a=new.sender_id then c.user_b else c.user_a end from public.conversations c where c.id=new.conversation_id on conflict do nothing;
 return new;
end $$;
revoke all on function private.queue_notification() from public,anon,authenticated;
create trigger queue_notification after insert on public.messages for each row execute function private.queue_notification();
create function private.register_device(p_token text,p_remove boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 if p_remove then delete from public.device_tokens where user_id=u and token=p_token;
 else
 perform private.limit_action('device',10);
 insert into public.device_tokens(token,user_id) values(p_token,u) on conflict(token) do update set user_id=u,updated_at=now();
 end if;
end $$;
create function public.register_device(p_token text,p_remove boolean default false) returns void language sql security invoker set search_path='' as $$select private.register_device(p_token,p_remove)$$;
revoke all on function private.register_device(text,boolean),public.register_device(text,boolean) from public,anon;
grant execute on function private.register_device(text,boolean),public.register_device(text,boolean) to authenticated;

create function private.claim_notifications(p_sender uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare r jsonb;begin
 with candidates as (
 select o.message_id from private.notification_outbox o join public.messages m on m.id=o.message_id
 where (p_sender is null or m.sender_id=p_sender) and o.attempts<5 and ((o.state='pending' and o.available_at<=now()) or (o.state='processing' and o.lease_until<now()))
 order by o.available_at for update of o skip locked limit 20
 ), claimed as (
 update private.notification_outbox o set state='processing',attempts=attempts+1,lease_until=now()+interval '90 seconds' from candidates c where o.message_id=c.message_id returning o.message_id
 ) select coalesce(jsonb_agg(message_id),'[]') into r from claimed;
 return r;
end $$;
create function public.claim_notifications(p_sender uuid default null) returns jsonb language sql security invoker set search_path='' as $$select private.claim_notifications(p_sender)$$;
create function private.notification_targets(p_message uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare o private.notification_outbox;m public.messages;result jsonb;begin
 select * into o from private.notification_outbox where message_id=p_message and state='processing';
 if o.message_id is null then return null;end if;
 select * into m from public.messages where id=p_message;
 if exists(select 1 from public.blocks b where (b.actor=m.sender_id and b.target=o.recipient_id) or (b.target=m.sender_id and b.actor=o.recipient_id)) or exists(select 1 from public.conversation_state s where s.conversation_id=m.conversation_id and s.user_id=o.recipient_id and (s.muted or s.deleted_seq>=m.seq)) then return null;end if;
 select jsonb_build_object('tokens',(select coalesce(jsonb_agg(token),'[]') from public.device_tokens where user_id=o.recipient_id),
 'data',jsonb_build_object('conversationId',m.conversation_id::text,'messageId',m.id::text,'senderId',m.sender_id::text,'recipientId',o.recipient_id::text,
 'title',case when s.notification_previews then p.display_name else 'Liquid Chat' end,'body',case when s.notification_previews then left(m.body,180) else 'New message' end)) into result
 from public.profiles p join public.settings s on s.user_id=o.recipient_id where p.id=m.sender_id;
 return result;
end $$;
create function public.notification_targets(p_message uuid) returns jsonb language sql security invoker set search_path='' as $$select private.notification_targets(p_message)$$;
create function private.finish_notification(p_message uuid,p_ok boolean,p_error text) returns void language plpgsql security definer set search_path='' as $$
begin
 update private.notification_outbox set state=case when p_ok then 'sent' when attempts>=5 then 'failed' else 'pending' end,
 available_at=now()+make_interval(secs=least(1800,(30*power(2,attempts))::int)),lease_until=null,last_error=left(p_error,80) where message_id=p_message and state='processing';
end $$;
create function public.finish_notification(p_message uuid,p_ok boolean,p_error text default '') returns void language sql security invoker set search_path='' as $$select private.finish_notification(p_message,p_ok,p_error)$$;
create function private.remove_invalid_device(p_token text,p_recipient uuid) returns void language plpgsql security definer set search_path='' as $$begin delete from public.device_tokens where token=p_token and user_id=p_recipient;end $$;
create function public.remove_invalid_device(p_token text,p_recipient uuid) returns void language sql security invoker set search_path='' as $$select private.remove_invalid_device(p_token,p_recipient)$$;
revoke all on function private.claim_notifications(uuid),private.notification_targets(uuid),private.finish_notification(uuid,boolean,text),private.remove_invalid_device(text,uuid),public.claim_notifications(uuid),public.notification_targets(uuid),public.finish_notification(uuid,boolean,text),public.remove_invalid_device(text,uuid) from public,anon,authenticated;
grant execute on function private.claim_notifications(uuid),private.notification_targets(uuid),private.finish_notification(uuid,boolean,text),private.remove_invalid_device(text,uuid),public.claim_notifications(uuid),public.notification_targets(uuid),public.finish_notification(uuid,boolean,text),public.remove_invalid_device(text,uuid) to service_role;

create extension if not exists pg_cron with schema pg_catalog;
create extension if not exists pg_net with schema extensions;
select vault.create_secret(gen_random_uuid()::text||gen_random_uuid()::text,'liquid_dispatch_secret');
create function private.dispatch_secret() returns text language sql stable security definer set search_path='' as $$select decrypted_secret from vault.decrypted_secrets where name='liquid_dispatch_secret' limit 1$$;
create function public.dispatch_secret() returns text language sql security invoker set search_path='' as $$select private.dispatch_secret()$$;
revoke all on function private.dispatch_secret(),public.dispatch_secret() from public,anon,authenticated;
grant execute on function private.dispatch_secret(),public.dispatch_secret() to service_role;
select cron.schedule('liquid-notification-dispatch','*/5 * * * *',$cron$
 select net.http_post(url:='https://qsdmspylumxysczzqnqc.supabase.co/functions/v1/notify',headers:=jsonb_build_object('Content-Type','application/json','apikey','sb_publishable_EIfZYiBXr4XR5eTW6vcPxw_qPLsQs78'),body:=jsonb_build_object('cron_secret',private.dispatch_secret()))
 where exists(select 1 from private.notification_outbox where state in('pending','processing') and attempts<5);
$cron$);
