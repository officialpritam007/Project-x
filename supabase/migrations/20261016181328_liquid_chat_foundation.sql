create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null unique check (username ~ '^[a-z0-9_]{3,24}$'),
  display_name text not null check (char_length(trim(display_name)) between 1 and 60),
  bio text not null default '' check (char_length(bio) <= 160),
  avatar_id text,
  created_at timestamptz not null default now()
);
create table public.settings (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  show_presence boolean not null default true,
  read_receipts boolean not null default true,
  notification_previews boolean not null default true
);
create table public.conversations (
  id uuid primary key default gen_random_uuid(),
  user_a uuid not null references public.profiles(id),
  user_b uuid not null references public.profiles(id),
  created_at timestamptz not null default now(),
  last_seq bigint not null default 0,
  check (user_a < user_b), unique(user_a,user_b)
);
create index conversations_user_b_idx on public.conversations(user_b);
create table public.conversation_state (
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  user_id uuid not null references public.profiles(id),
  favorite boolean not null default false,
  archived boolean not null default false,
  muted boolean not null default false,
  delivered_seq bigint not null default 0,
  read_seq bigint not null default 0,
  deleted_seq bigint not null default 0,
  primary key(conversation_id,user_id)
);
create index state_user_idx on public.conversation_state(user_id,conversation_id);
create table public.messages (
  id uuid primary key,
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id uuid not null references public.profiles(id),
  seq bigint generated always as identity unique,
  body text not null check (char_length(body) between 1 and 4000),
  created_at timestamptz not null default now()
);
create index messages_history_idx on public.messages(conversation_id,seq desc);
create index messages_sender_idx on public.messages(sender_id,created_at desc);
create table public.blocks (
  actor uuid not null references public.profiles(id) on delete cascade,
  target uuid not null references public.profiles(id) on delete cascade,
  primary key(actor,target), check(actor<>target)
);
create index blocks_target_idx on public.blocks(target);
create table private.rate_limits (
  user_id uuid not null references auth.users(id) on delete cascade,
  action text not null, bucket timestamptz not null, n int not null,
  primary key(user_id,action)
);
alter table private.rate_limits enable row level security;

-- Private privileged routines are not exposed by PostgREST. Public wrappers use INVOKER.
create function private.require_user() returns uuid language plpgsql stable security invoker set search_path='' as $$
begin
 if auth.uid() is null then raise exception 'Sign in required' using errcode='42501'; end if;
 return auth.uid();
end $$;
create function private.limit_action(p_action text,p_max int) returns void language plpgsql security definer set search_path='' as $$
declare n int; u uuid:=private.require_user();
begin
 insert into private.rate_limits(user_id,action,bucket,n) values(u,p_action,date_trunc('minute',now()),1)
 on conflict(user_id,action) do update set
 n=case when rate_limits.bucket=date_trunc('minute',now()) then rate_limits.n+1 else 1 end,
 bucket=date_trunc('minute',now()) returning rate_limits.n into n;
 if n>p_max then raise exception 'Please wait a minute before trying again' using errcode='P0001'; end if;
end $$;
create function private.is_member(cid uuid) returns boolean language sql stable security definer set search_path='' as $$
 select auth.uid() is not null and exists(select 1 from public.conversations c where c.id=cid and auth.uid() in (c.user_a,c.user_b));
$$;
create function private.is_blocked(peer uuid) returns boolean language sql stable security definer set search_path='' as $$
 select auth.uid() is null or exists(select 1 from public.blocks b where (b.actor=auth.uid() and b.target=peer) or (b.target=auth.uid() and b.actor=peer));
$$;

alter table public.profiles enable row level security;
alter table public.settings enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_state enable row level security;
alter table public.messages enable row level security;
alter table public.blocks enable row level security;
revoke all on public.profiles,public.settings,public.conversations,public.conversation_state,public.messages,public.blocks from anon,authenticated;
grant select on public.profiles,public.settings,public.conversations,public.conversation_state,public.messages,public.blocks to authenticated;
create policy profiles_read on public.profiles for select to authenticated using (
 id=(select auth.uid()) or exists(select 1 from public.conversations c where (select auth.uid()) in(c.user_a,c.user_b) and profiles.id in(c.user_a,c.user_b))
);
create policy settings_owner on public.settings for select to authenticated using (user_id=(select auth.uid()));
create policy conversations_participant on public.conversations for select to authenticated using ((select auth.uid()) in(user_a,user_b));
create policy state_participant on public.conversation_state for select to authenticated using (private.is_member(conversation_id));
-- A user's deletion watermark also applies to direct REST history reads.
create policy messages_participant on public.messages for select to authenticated using (
 private.is_member(conversation_id) and seq > coalesce((select s.deleted_seq from public.conversation_state s where s.conversation_id=messages.conversation_id and s.user_id=(select auth.uid())),0)
);
create policy blocks_owner on public.blocks for select to authenticated using(actor=(select auth.uid()));

create function private.save_profile(p_username text,p_name text,p_bio text) returns public.profiles language plpgsql security definer set search_path='' as $$
declare r public.profiles; u uuid:=private.require_user();
begin
 perform private.limit_action('profile',10);
 insert into public.profiles(id,username,display_name,bio) values(u,lower(trim(p_username)),trim(p_name),p_bio)
 on conflict(id) do update set username=excluded.username,display_name=excluded.display_name,bio=excluded.bio returning * into r;
 insert into public.settings(user_id) values(u) on conflict do nothing;
 return r;
end $$;
create function public.save_profile(p_username text,p_name text,p_bio text default '') returns public.profiles language sql security invoker set search_path='' as $$select private.save_profile(p_username,p_name,p_bio)$$;

create function private.find_profile(p_username text) returns setof public.profiles language plpgsql security definer set search_path='' as $$
begin
 perform private.limit_action('discovery',20);
 return query select p.* from public.profiles p where p.username=lower(trim(p_username)) and not private.is_blocked(p.id) limit 1;
end $$;
create function public.find_profile(p_username text) returns setof public.profiles language sql security invoker set search_path='' as $$select * from private.find_profile(p_username)$$;

create function private.start_conversation(p_peer uuid) returns uuid language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); cid uuid;
begin
 if u=p_peer or private.is_blocked(p_peer) then raise exception 'Contact unavailable' using errcode='42501'; end if;
 perform private.limit_action('conversation',20);
 insert into public.conversations(user_a,user_b) values(least(u,p_peer),greatest(u,p_peer)) on conflict(user_a,user_b) do nothing;
 select id into cid from public.conversations where user_a=least(u,p_peer) and user_b=greatest(u,p_peer);
 insert into public.conversation_state(conversation_id,user_id) values(cid,u),(cid,p_peer) on conflict do nothing;
 return cid;
end $$;
create function public.start_conversation(p_peer uuid) returns uuid language sql security invoker set search_path='' as $$select private.start_conversation(p_peer)$$;

create function private.send_text(p_id uuid,p_conversation uuid,p_body text) returns public.messages language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); c public.conversations; m public.messages; peer uuid;
begin
 select * into c from public.conversations where id=p_conversation for update;
 if c.id is null or u not in(c.user_a,c.user_b) then raise exception 'Conversation unavailable' using errcode='42501'; end if;
 peer:=case when u=c.user_a then c.user_b else c.user_a end;
 -- Retry of a previously accepted message remains idempotent, even after a block.
 select * into m from public.messages where id=p_id;
 if m.id is not null then
   if m.sender_id<>u or m.conversation_id<>p_conversation or m.body<>p_body then raise exception 'Message ID conflict' using errcode='23505'; end if;
   return m;
 end if;
 if private.is_blocked(peer) then raise exception 'Contact unavailable' using errcode='42501'; end if;
 perform private.limit_action('send',60);
 insert into public.messages(id,conversation_id,sender_id,body) values(p_id,p_conversation,u,p_body) returning * into m;
 update public.conversations set last_seq=m.seq where id=c.id;
 return m;
end $$;
create function public.send_text(p_id uuid,p_conversation uuid,p_body text) returns public.messages language sql security invoker set search_path='' as $$select private.send_text(p_id,p_conversation,p_body)$$;

create function private.acknowledge(p_conversation uuid,p_seq bigint,p_read boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); max_seq bigint; receipts boolean;
begin
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501'; end if;
 select coalesce(max(seq),0) into max_seq from public.messages where conversation_id=p_conversation and sender_id<>u;
 if p_seq<0 or p_seq>max_seq then raise exception 'Invalid receipt'; end if;
 select read_receipts into receipts from public.settings where user_id=u;
 update public.conversation_state set delivered_seq=greatest(delivered_seq,p_seq), read_seq=case when p_read and receipts then greatest(read_seq,p_seq) else read_seq end where conversation_id=p_conversation and user_id=u;
end $$;
create function public.acknowledge(p_conversation uuid,p_seq bigint,p_read boolean) returns void language sql security invoker set search_path='' as $$select private.acknowledge(p_conversation,p_seq,p_read)$$;

create function private.set_conversation(p_conversation uuid,p_favorite boolean,p_archived boolean,p_muted boolean,p_delete boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); latest bigint;
begin
 select last_seq into latest from public.conversations where id=p_conversation for update;
 if not private.is_member(p_conversation) then raise exception 'Conversation unavailable' using errcode='42501'; end if;
 update public.conversation_state set favorite=p_favorite,archived=p_archived,muted=p_muted, deleted_seq=case when p_delete then latest else deleted_seq end where conversation_id=p_conversation and user_id=u;
end $$;
create function public.set_conversation(p_conversation uuid,p_favorite boolean,p_archived boolean,p_muted boolean,p_delete boolean default false) returns void language sql security invoker set search_path='' as $$select private.set_conversation(p_conversation,p_favorite,p_archived,p_muted,p_delete)$$;

create function private.set_block(p_peer uuid,p_blocked boolean) returns void language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();
begin
 if u=p_peer then raise exception 'Invalid contact'; end if;
 -- Serialize against sends involving this pair, so a completed block rejects later sends.
 perform 1 from public.conversations where user_a=least(u,p_peer) and user_b=greatest(u,p_peer) for update;
 if p_blocked then insert into public.blocks(actor,target) values(u,p_peer) on conflict do nothing;
 else delete from public.blocks where actor=u and target=p_peer; end if;
end $$;
create function public.set_block(p_peer uuid,p_blocked boolean) returns void language sql security invoker set search_path='' as $$select private.set_block(p_peer,p_blocked)$$;

-- No default PUBLIC function grants; only named app entrypoints are callable.
revoke all on all functions in schema private from public,anon,authenticated;
grant execute on function private.require_user(),private.is_member(uuid),private.is_blocked(uuid),private.save_profile(text,text,text),private.find_profile(text),private.start_conversation(uuid),private.send_text(uuid,uuid,text),private.acknowledge(uuid,bigint,boolean),private.set_conversation(uuid,boolean,boolean,boolean,boolean),private.set_block(uuid,boolean) to authenticated;
revoke all on function public.save_profile(text,text,text),public.find_profile(text),public.start_conversation(uuid),public.send_text(uuid,uuid,text),public.acknowledge(uuid,bigint,boolean),public.set_conversation(uuid,boolean,boolean,boolean,boolean),public.set_block(uuid,boolean) from public,anon;
grant execute on function public.save_profile(text,text,text),public.find_profile(text),public.start_conversation(uuid),public.send_text(uuid,uuid,text),public.acknowledge(uuid,bigint,boolean),public.set_conversation(uuid,boolean,boolean,boolean,boolean),public.set_block(uuid,boolean) to authenticated;
