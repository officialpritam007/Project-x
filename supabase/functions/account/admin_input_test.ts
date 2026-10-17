import { createClient } from "npm:@supabase/supabase-js@2.52.1";

const userId = "f0000000-0000-4000-8000-000000000001";

Deno.test("admin deletion rejects malformed IDs before any HTTP request", async () => {
  let requests = 0;
  const client = createClient("https://fixture.invalid", "fixture-key", {
    auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false },
    global: { fetch: () => { requests++; throw new Error("Unexpected fixture request"); } },
  });
  for (const id of ["../logout", "../users/" + userId, userId + "/factors", "", "not-a-uuid"]) {
    let rejected = false;
    try {
      const result = await client.auth.admin.deleteUser(id);
      rejected = result.error != null;
    } catch { rejected = true; }
    if (!rejected) throw new Error("Malformed deletion ID was accepted");
  }
  if (requests !== 0) throw new Error("Malformed admin ID reached HTTP routing");
});

Deno.test("admin deletion preserves routing for a valid job UUID", async () => {
  const calls: string[] = [];
  const client = createClient("https://fixture.invalid", "fixture-key", {
    auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false },
    global: {
      fetch: (input, options) => {
        calls.push(String(input));
        if (options?.method !== "DELETE") throw new Error("Wrong admin operation");
        return Promise.resolve(new Response(JSON.stringify({ id: userId, aud: "authenticated", created_at: "2026-01-01T00:00:00Z" }), {
          status: 200, headers: { "Content-Type": "application/json" },
        }));
      },
    },
  });
  const result = await client.auth.admin.deleteUser(userId);
  if (result.error || result.data.user?.id !== userId) throw new Error("Valid UUID deletion contract changed");
  if (calls.length !== 1 || calls[0] !== "https://fixture.invalid/auth/v1/admin/users/" + userId) {
    throw new Error("Unexpected admin route");
  }
});
