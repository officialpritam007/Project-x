create function private.erase_message_preferences() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.is_deleted and not old.is_deleted then
 delete from private.message_preferences where user_id=new.id;
 end if;
 return new;
end $$;
revoke all on function private.erase_message_preferences() from public,anon,authenticated;
create trigger erase_message_preferences after update of is_deleted on public.profiles for each row execute function private.erase_message_preferences();
