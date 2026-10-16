alter policy state_participant on public.conversation_state using(user_id=(select auth.uid()));
create policy internal_rate_limits_denied on private.rate_limits for all to authenticated using(false) with check(false);
