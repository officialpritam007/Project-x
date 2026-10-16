create function private.session_active() returns boolean language sql stable security definer set search_path='' as $$
 select auth.uid() is not null and exists(select 1 from auth.sessions s where s.id=nullif(auth.jwt()->>'session_id','')::uuid and s.user_id=auth.uid() and (s.not_after is null or s.not_after>now()));
$$;
revoke all on function private.session_active() from public,anon;
grant execute on function private.session_active() to authenticated;
create or replace function private.require_user() returns uuid language plpgsql stable security invoker set search_path='' as $$
begin
 if not private.session_active() then raise exception 'Sign in required' using errcode='42501';end if;return auth.uid();
end $$;
create or replace function private.is_member(cid uuid) returns boolean language sql stable security definer set search_path='' as $$
 select private.session_active() and exists(select 1 from public.conversations c where c.id=cid and auth.uid() in(c.user_a,c.user_b));
$$;
create policy active_session_profiles on public.profiles as restrictive for select to authenticated using((select private.session_active()));
create policy active_session_settings on public.settings as restrictive for select to authenticated using((select private.session_active()));
create policy active_session_conversations on public.conversations as restrictive for select to authenticated using((select private.session_active()));
create policy active_session_state on public.conversation_state as restrictive for select to authenticated using((select private.session_active()));
create policy active_session_messages on public.messages as restrictive for select to authenticated using((select private.session_active()));
create policy active_session_blocks on public.blocks as restrictive for select to authenticated using((select private.session_active()));
create policy active_session_devices on public.device_tokens as restrictive for select to authenticated using((select private.session_active()));
create index images_owner_idx on private.images(owner_id);
create index images_conversation_idx on private.images(conversation_id);
create index messages_media_idx on public.messages(media_id) where media_id is not null;
