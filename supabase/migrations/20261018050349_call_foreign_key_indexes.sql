-- Cover foreign keys used by account erasure and conversation deletion.
-- No user-visible data changes and no paid services.
create index if not exists call_candidates_sender_idx on public.call_candidates(sender_id);
create index if not exists call_sessions_conversation_idx on public.call_sessions(conversation_id);
