create or replace function private.send_image(p_id uuid,p_media uuid,p_caption text) returns public.messages language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();i private.images;m public.messages;c public.conversations;caption text:=coalesce(nullif(trim(p_caption),''),'Photo');begin
 select * into i from private.images where id=p_media for update;
 if i.owner_id is distinct from u or not i.ready or i.conversation_id is null then raise exception 'Unavailable' using errcode='42501';end if;
 select * into c from public.conversations where id=i.conversation_id for update;
 if not private.is_member(i.conversation_id) then raise exception 'Unavailable' using errcode='42501';end if;
 select * into m from public.messages where id=p_id;
 if m.id is not null then
 if m.media_id is distinct from i.id or m.sender_id<>u or m.conversation_id<>i.conversation_id or m.body<>caption then raise exception 'Message conflict' using errcode='23505';end if;
 return m;end if;
 if i.published or private.is_blocked(case when c.user_a=u then c.user_b else c.user_a end) then raise exception 'Unavailable' using errcode='42501';end if;
 perform private.limit_action('send',60);
 insert into public.messages(id,conversation_id,sender_id,body,media_id,image_width,image_height) values(p_id,i.conversation_id,u,caption,i.id,i.width,i.height) returning * into m;
 update public.conversations set last_seq=m.seq where id=i.conversation_id;
 update private.images set published=true where id=i.id;
 return m;
end $$;
create or replace function private.list_chats(p_before_seq bigint,p_before_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user(); result jsonb;
begin
 select coalesce(jsonb_agg(row_to_json(t)::jsonb order by t.last_seq desc,t.id desc),'[]') into result from (
 select c.id,c.last_seq,c.user_a,c.user_b,p.username,p.display_name,p.avatar_id,
 s.favorite,s.archived,s.muted,s.deleted_seq,s.read_seq,
 peer.delivered_seq as peer_delivered_seq,peer.read_seq as peer_read_seq,
 (select count(*) from public.messages m where m.conversation_id=c.id and m.sender_id<>u and m.seq>greatest(s.read_seq,s.deleted_seq)) as unread,
 (select m.created_at from public.messages m where m.conversation_id=c.id and m.seq>s.deleted_seq order by m.seq desc limit 1) as last_at,
 (select left(m.body,160) from public.messages m where m.conversation_id=c.id and m.seq>s.deleted_seq order by m.seq desc limit 1) as preview
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

