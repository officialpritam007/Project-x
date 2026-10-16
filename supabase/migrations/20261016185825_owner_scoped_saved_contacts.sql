create table public.contacts(owner_id uuid not null references public.profiles(id),target_id uuid not null references public.profiles(id),created_at timestamptz not null default now(),primary key(owner_id,target_id),check(owner_id<>target_id));
create index contacts_target_idx on public.contacts(target_id);
alter table public.contacts enable row level security;
revoke all on public.contacts from public,anon,authenticated;
grant select on public.contacts to authenticated;
create policy contacts_owner on public.contacts for select to authenticated using(owner_id=(select auth.uid()) and (select private.session_active()));
create function private.save_contact(p_peer uuid,p_saved boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();begin
 if p_saved then
 if u=p_peer or private.is_blocked(p_peer) then raise exception 'Contact unavailable' using errcode='42501';end if;
 perform private.limit_action('contact',20);
 insert into public.contacts(owner_id,target_id) values(u,p_peer) on conflict do nothing;
 else delete from public.contacts where owner_id=u and target_id=p_peer;end if;
end $$;
create function public.save_contact(p_peer uuid,p_saved boolean default true) returns void language sql security invoker set search_path='' as $$select private.save_contact(p_peer,p_saved)$$;
create function private.list_contacts() returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();r jsonb;begin
 select coalesce(jsonb_agg(row_to_json(t)),'[]') into r from (select p.id,p.display_name,p.username,p.avatar_id,p.bio from public.contacts c join public.profiles p on p.id=c.target_id where c.owner_id=u and not p.is_deleted order by p.display_name,p.id limit 1000)t;return r;
end $$;
create function public.list_contacts() returns jsonb language sql security invoker set search_path='' as $$select private.list_contacts()$$;
revoke all on function private.save_contact(uuid,boolean),public.save_contact(uuid,boolean),private.list_contacts(),public.list_contacts() from public,anon;
grant execute on function private.save_contact(uuid,boolean),public.save_contact(uuid,boolean),private.list_contacts(),public.list_contacts() to authenticated;
