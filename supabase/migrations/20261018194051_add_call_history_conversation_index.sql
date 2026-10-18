-- Applied to Supabase Free project qsdmspylumxysczzqnqc as migration 20261018194051.
-- Additive index only; preserves all existing call history rows.
-- Covers call_history.conversation_id foreign key and conversation cleanup lookups.
create index if not exists call_history_conversation_id_idx
    on public.call_history (conversation_id);
