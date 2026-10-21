-- Read-only verification contract; do not execute until the user
-- authorizes tests and the chat-video migration is staged in an isolated DB.
-- Requires 20261021230500_private_chat_videos.sql.
begin;
do $checks$
declare
  image_definition text;
  message_definition text;
  shape_definition text;
begin
  select pg_get_constraintdef(oid) into image_definition from pg_constraint
    where conrelid='private.images'::regclass and conname='images_kind_check';
  select pg_get_constraintdef(oid) into message_definition from pg_constraint
    where conrelid='public.messages'::regclass and conname='messages_media_kind_check';
  select pg_get_constraintdef(oid) into shape_definition from pg_constraint
    where conrelid='private.images'::regclass and conname='chat_video_ready_shape';
  assert image_definition like '%chat_video%', 'Private chat video kind constraint missing';
  assert message_definition like '%chat_video%', 'Message chat video kind constraint missing';
  assert shape_definition like '%8388608%' and shape_definition like '%30000%',
    'Video length/bytes server constraints missing';
  assert to_regprocedure('public.send_chat_video(uuid,uuid,text)') is not null,
    'Participant chat-video send RPC is missing';
  assert to_regprocedure('public.finalize_chat_video(uuid,integer,integer,integer,integer)') is not null,
    'Service-role chat-video finalizer is missing';
  assert has_function_privilege('authenticated',
    'public.send_chat_video(uuid,uuid,text)','EXECUTE'),
    'Authenticated members cannot call send_chat_video';
  assert not has_function_privilege('anon',
    'public.send_chat_video(uuid,uuid,text)','EXECUTE'),
    'Anon must not call chat-video send';
  assert not has_function_privilege('authenticated',
    'public.finalize_chat_video(uuid,integer,integer,integer,integer)','EXECUTE'),
    'Users must not finalize uninspected video payloads';
  assert not has_function_privilege('anon',
    'public.finalize_chat_video(uuid,integer,integer,integer,integer)','EXECUTE'),
    'Anon must not finalize videos';
  assert has_function_privilege('service_role',
    'public.finalize_chat_video(uuid,integer,integer,integer,integer)','EXECUTE'),
    'Service-role finalizer unavailable';
end
$checks$;
rollback;
