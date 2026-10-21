-- Read-only contract assertions for the new media and edit-history migrations.
-- Run only AFTER migrations 20261021183500, 20261021195500 and
-- 20261021202500 have been applied on an isolated verification database.
-- This file does not send messages, mutate user rows or access stored files.
begin;

do $contract$
declare
  image_kinds text;
  message_kinds text;
begin
  select pg_get_constraintdef(oid) into image_kinds
    from pg_constraint
    where conrelid='private.images'::regclass and conname='images_kind_check';
  assert image_kinds is not null and image_kinds like '%sticker%'
      and image_kinds like '%document%', 'Private media kind constraints absent';

  select pg_get_constraintdef(oid) into message_kinds
    from pg_constraint
    where conrelid='public.messages'::regclass and conname='messages_media_kind_check';
  assert message_kinds is not null and message_kinds like '%sticker%'
      and message_kinds like '%document%', 'Message kind constraints absent';

  assert to_regprocedure('public.send_sticker(uuid,uuid)') is not null,
      'Sticker RPC missing';
  assert to_regprocedure('public.send_document(uuid,uuid,text)') is not null,
      'PDF RPC missing';
  assert to_regprocedure('public.finalize_sticker(uuid,integer)') is not null,
      'Sticker finalizer missing';
  assert to_regprocedure('public.finalize_document(uuid,integer)') is not null,
      'PDF finalizer missing';

  assert not has_function_privilege('anon','public.send_sticker(uuid,uuid)','EXECUTE'),
      'Anonymous users must not send stickers';
  assert not has_function_privilege('anon','public.send_document(uuid,uuid,text)','EXECUTE'),
      'Anonymous users must not send documents';
  assert has_function_privilege('authenticated','public.send_sticker(uuid,uuid)','EXECUTE'),
      'Authenticated sticker send unavailable';
  assert has_function_privilege('authenticated','public.send_document(uuid,uuid,text)','EXECUTE'),
      'Authenticated PDF send unavailable';

  assert not has_function_privilege('authenticated',
      'public.finalize_sticker(uuid,integer)','EXECUTE'),
      'Sticker finalizer must be service-role only';
  assert not has_function_privilege('authenticated',
      'public.finalize_document(uuid,integer)','EXECUTE'),
      'PDF finalizer must be service-role only';
  assert has_function_privilege('service_role',
      'public.finalize_document(uuid,integer)','EXECUTE'),
      'Service role cannot finalize PDFs';
  assert has_function_privilege('service_role',
      'public.finalize_sticker(uuid,integer)','EXECUTE'),
      'Service role cannot finalize stickers';

  assert to_regprocedure('public.message_edit_history(uuid)') is not null,
      'Edit-history endpoint missing';
  assert has_function_privilege('authenticated',
      'public.message_edit_history(uuid)','EXECUTE'),
      'Participant history entrypoint missing';
  assert not has_function_privilege('anon',
      'public.message_edit_history(uuid)','EXECUTE'),
      'Anonymous edit history must be denied';
  assert (select relrowsecurity from pg_class
      where oid='private.message_edit_versions'::regclass),
      'Private history table must enforce RLS';
  assert not has_table_privilege('authenticated',
      'private.message_edit_versions','SELECT'),
      'Authenticated users must never select revision rows directly';
end
$contract$;

rollback;
