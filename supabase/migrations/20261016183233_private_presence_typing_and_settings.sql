create table private.presence(user_id uuid primary key references public.profiles(id) on delete cascade, online boolean not null, last_seen timestamptz not null, expires_at timestamptz not null);
create table private.typing(conversation_id uuid not null references public.conversations(id) on delete cascade,user_id uuid not null references public.profiles(id) on delete cascade,expires_at timestamptz not null,primary key(conversation_id,user_id));
alter table private.presence enable row level security;
alter table private.typing enable row level security;
create policy presence_internal on private.presence for all to authenticated using(false) with check(false);
create policy typing_internal on private.typing for all to authenticated using(false) with check(false);
create function private.update_presence(p_online boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();
begin
 insert into private.presence values(u,p_online,now(),now()+interval '30 seconds') on conflict(user_id) do update set online=p_online,last_seen=now(),expires_at=now()+interval '30 seconds';
end $$;
create function public.update_presence(p_online boolean) returns void language sql security invoker set search_path='' as $$select private.update_presence(p_online)$$;
create function private.update_typing(p_conversation uuid,p_typing boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); peer uuid;
begin
 select case when user_a=u then user_b else user_a end into peer from public.conversations where id=p_conversation;
 if not private.is_member(p_conversation) or private.is_blocked(peer) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 if p_typing then perform private.limit_action('typing',30);end if;
 insert into private.typing values(p_conversation,u,case when p_typing then now()+interval '5 seconds' else now() end) on conflict(conversation_id,user_id) do update set expires_at=excluded.expires_at;
end $$;
create function public.update_typing(p_conversation uuid,p_typing boolean) returns void language sql security invoker set search_path='' as $$select private.update_typing(p_conversation,p_typing)$$;
create function private.contact_state(p_conversation uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();peer uuid;r jsonb;
begin
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501';end if;
 select case when user_a=u then user_b else user_a end into peer from public.conversations where id=p_conversation;
 if private.is_blocked(peer) then return jsonb_build_object('online',false,'last_seen',null,'typing',false);end if;
 select jsonb_build_object('online',coalesce(s.show_presence and p.online and p.expires_at>now(),false),
 'last_seen',case when s.show_presence then p.last_seen else null end,
 'typing',exists(select 1 from private.typing t where t.conversation_id=p_conversation and t.user_id=peer and t.expires_at>now()))
 into r from public.settings s left join private.presence p on p.user_id=s.user_id where s.user_id=peer;
 return coalesce(r,'{}');
end $$;
create function public.contact_state(p_conversation uuid) returns jsonb language sql security invoker set search_path='' as $$select private.contact_state(p_conversation)$$;
create function private.save_settings(p_presence boolean,p_receipts boolean,p_previews boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 update public.settings set show_presence=p_presence,read_receipts=p_receipts,notification_previews=p_previews where user_id=u;
end $$;
create function public.save_settings(p_presence boolean,p_receipts boolean,p_previews boolean) returns void language sql security invoker set search_path='' as $$select private.save_settings(p_presence,p_receipts,p_previews)$$;
revoke all on function private.update_presence(boolean),private.update_typing(uuid,boolean),private.contact_state(uuid),private.save_settings(boolean,boolean,boolean) from public,anon;
revoke all on function public.update_presence(boolean),public.update_typing(uuid,boolean),public.contact_state(uuid),public.save_settings(boolean,boolean,boolean) from public,anon;
grant execute on function private.update_presence(boolean),private.update_typing(uuid,boolean),private.contact_state(uuid),private.save_settings(boolean,boolean,boolean),public.update_presence(boolean),public.update_typing(uuid,boolean),public.contact_state(uuid),public.save_settings(boolean,boolean,boolean) to authenticated;
