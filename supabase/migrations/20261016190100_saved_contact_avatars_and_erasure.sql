create or replace function private.image_access(p_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;begin
 select * into i from private.images where id=p_id and ready;
 if i.id is null then raise exception 'Unavailable' using errcode='42501';end if;
 if i.owner_id<>u then
 if i.conversation_id is null then
 if (not exists(select 1 from public.profiles p join public.conversations c on p.id in(c.user_a,c.user_b) where p.avatar_id=i.id::text and u in(c.user_a,c.user_b)) and not exists(select 1 from public.contacts where owner_id=u and target_id=i.owner_id)) or private.is_blocked(i.owner_id) then raise exception 'Unavailable' using errcode='42501';end if;
 else
 if private.is_blocked(i.owner_id) or not private.is_member(i.conversation_id) or not exists(select 1 from public.messages m join public.conversation_state s on s.conversation_id=m.conversation_id and s.user_id=u where m.media_id=i.id and m.seq>s.deleted_seq) then raise exception 'Unavailable' using errcode='42501';end if;
 end if;
 end if;
 return jsonb_build_object('public_id',i.public_id,'format',i.format,'width',i.width,'height',i.height);
end $$;
create or replace function private.finish_deletion(p_job uuid) returns boolean language plpgsql security definer set search_path='' as $$
declare u uuid;begin
 select user_id into u from private.deletion_jobs where id=p_job for update;
 if u is null or exists(select 1 from private.images where owner_id=u and not destroyed) or exists(select 1 from auth.users where id=u) then return false;end if;
 delete from public.device_tokens where user_id=u;
 delete from public.contacts where owner_id=u or target_id=u;
 delete from private.presence where user_id=u;
 delete from private.typing where user_id=u;
 delete from public.blocks where actor=u or target=u;
 update public.conversation_state set favorite=false,archived=false,muted=true where user_id=u;
 update private.notification_outbox o set state='discarded' where o.recipient_id=u or exists(select 1 from public.messages m where m.id=o.message_id and m.sender_id=u);
 update private.deletion_jobs set state='done' where id=p_job;
 return true;
end $$;
