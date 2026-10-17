import { createClient } from "npm:@supabase/supabase-js@2.52.1";
const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false } });
const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" } });
let cachedToken = "", tokenExpiry = 0;
const b64 = (input: Uint8Array) => btoa(String.fromCharCode(...input)).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
async function accessToken(credentials: { client_email: string; private_key: string }) {
  if (cachedToken && tokenExpiry > Date.now()) return cachedToken;
  const now = Math.floor(Date.now() / 1000), encode = (x: unknown) => b64(new TextEncoder().encode(JSON.stringify(x)));
  const unsigned = `${encode({ alg: "RS256", typ: "JWT" })}.${encode({ iss: credentials.client_email, scope: "https://www.googleapis.com/auth/firebase.messaging", aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600 })}`;
  const pem = credentials.private_key.replace(/-----[^-]+-----/g, "").replace(/\s/g, "");
  const key = await crypto.subtle.importKey("pkcs8", Uint8Array.from(atob(pem), x => x.charCodeAt(0)), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signed = b64(new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned))));
  const response = await fetch("https://oauth2.googleapis.com/token", { method: "POST", body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${unsigned}.${signed}` }), signal: AbortSignal.timeout(8000) });
  if (!response.ok) throw new Error("FCM authorization unavailable");
  const data = await response.json(); if (typeof data.access_token !== "string") throw new Error("FCM authorization unavailable");
  cachedToken = data.access_token; tokenExpiry = Date.now() + 3300000; return cachedToken;
}
Deno.serve(async req => {
  if (req.method !== "POST") return reply({ error: "Method not allowed" }, 405);
  let sender: string | null = null;
  try {
    const reader = req.body?.getReader(); let raw = "";
    if (reader) while (true) { const { value, done } = await reader.read(); if (done) break; raw += new TextDecoder().decode(value); if (raw.length > 4096) { await reader.cancel(); return reply({ error: "Invalid request" }, 400); } }
    const body = JSON.parse(raw || "{}");
    if (typeof body.cron_secret === "string") {
      const { data: expected, error } = await admin.rpc("dispatch_secret");
      const a = new TextEncoder().encode(body.cron_secret), b = new TextEncoder().encode(expected ?? "");
      let mismatch = a.length ^ b.length; for (let i = 0; i < Math.max(a.length, b.length); i++) mismatch |= (a[i] ?? 0) ^ (b[i] ?? 0);
      if (error || !b.length || mismatch) return reply({ error: "Unauthorized" }, 401);
    } else {
      const jwt = req.headers.get("Authorization")?.replace(/^Bearer /i, "");
      if (!jwt) return reply({ error: "Sign in required" }, 401);
      const { data, error } = await admin.auth.getUser(jwt);
      if (error || !data.user) return reply({ error: "Sign in required" }, 401);
      sender = data.user.id;
    }
    const secret = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");
    if (!secret) return reply({ error: "Push delivery is temporarily unavailable" }, 503);
    const credentials = JSON.parse(secret);
    if (!/^[a-z0-9-]+$/.test(credentials.project_id)) throw new Error("Invalid FCM configuration");
    const token = await accessToken(credentials);
    const { data: jobs, error } = await admin.rpc("claim_notifications", { p_sender: sender });
    if (error) throw new Error("Queue unavailable");
    let processed = 0;
    async function dispatch(id: string) {
      let ok = true;
      try {
        const { data: target, error } = await admin.rpc("notification_targets", { p_message: id });
        if (error) throw new Error("Queue unavailable");
        if (target) await Promise.all(target.tokens.map(async (device: string) => {
          const res = await fetch(`https://fcm.googleapis.com/v1/projects/${credentials.project_id}/messages:send`, { method: "POST", headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" }, body: JSON.stringify({ message: { token: device, data: target.data, android: { priority: "high", ttl: "86400s" } } }), signal: AbortSignal.timeout(8000) });
          if (!res.ok) {
            const failure = await res.json().catch(() => ({}));
            const invalid = failure.error?.details?.some((x: { errorCode?: string }) => x.errorCode === "UNREGISTERED");
            if (invalid) await admin.rpc("remove_invalid_device", { p_token: device, p_recipient: target.data.recipientId });
            else ok = false;
          }
        }));
      } catch { ok = false; }
      await admin.rpc("finish_notification", { p_message: id, p_ok: ok, p_error: ok ? "" : "FCM_TEMPORARY_FAILURE" });
      processed++;
    }
    for (let i = 0; i < jobs.length; i += 4) await Promise.all(jobs.slice(i, i + 4).map(dispatch));
    return reply({ processed });
  } catch { return reply({ error: "Push service unavailable" }, 503); }
});
