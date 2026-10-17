-- Additive only: no existing objects, migrations, rows or grants are replaced.
create function private.report_moment(p_id uuid,p_reason text) returns void
language plpgsql security definer set search_path='' as $$
declare u uuid:=private.require_user();m private.moments;begin
 if p_reason is null or char_length(trim(p_reason)) not between 1 and 800 then
  raise exception 'Provide a reason between 1 and 800 characters' using errcode='22023';
 end if;
 select x.* into m from private.moments x
 join public.profiles p on p.id=x.owner_id and not p.is_deleted
 join private.images i on i.id=x.media_id and i.ready and not i.destroyed
 where x.id=p_id and not x.archived and (x.expires_at is null or x.expires_at>now());
 if m.id is null or m.owner_id=u or not private.moment_visible(m.owner_id) then
  raise exception 'Unavailable' using errcode='42501';
 end if;
 perform private.limit_action('report',2);
 -- Bind the evidence ID server-side; do not copy photo/voice content into logs.
 insert into private.reports(actor,target,reason)
 values(u,m.owner_id,format('[%s:%s] %s',m.kind,m.id,trim(p_reason)));
end $$;
create function public.report_moment(p_id uuid,p_reason text) returns void
language sql security invoker set search_path='' as $$select private.report_moment(p_id,p_reason)$$;
revoke all on function private.report_moment(uuid,text),public.report_moment(uuid,text) from public,anon;
grant execute on function private.report_moment(uuid,text),public.report_moment(uuid,text) to authenticated;
