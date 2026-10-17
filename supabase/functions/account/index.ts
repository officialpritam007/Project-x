import { createClient } from "npm:@supabase/supabase-js@2.49.4";
const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false } });
const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" } });
async function processJob(id: string | null) {
  const { data: job, error } = await admin.rpc("deletion_batch", { p_job: id });
  if (error || !job) return false;
  const cloud = Deno.env.get("CLOUDINARY_CLOUD_NAME"), key = Deno.env.get("CLOUDINARY_API_KEY"), secret = Deno.env.get("CLOUDINARY_API_SECRET");
  for (const image of job.images) {
    if(image.provider==="supabase") {
      const {error}=await admin.storage.from("liquid-media").remove([image.public_id+"."+image.format]);
      if(error)return false;
    } else {
    if (!cloud || !key || !secret) return false;
    const params = { public_id: image.public_id, type: "authenticated", invalidate: "true", timestamp: Math.floor(Date.now() / 1000).toString() };
    const input = Object.keys(params).sort().map(k => `${k}=${params[k as keyof typeof params]}`).join("&") + secret;
    const signature = [...new Uint8Array(await crypto.subtle.digest("SHA-1", new TextEncoder().encode(input)))].map(x => x.toString(16).padStart(2, "0")).join("");
    const response = await fetch(`https://api.cloudinary.com/v1_1/${cloud}/${image.resource_type === "video" ? "video" : "image"}/destroy`, { method: "POST", body: new URLSearchParams({ ...params, api_key: key, signature }), signal: AbortSignal.timeout(8000) });
    const result = await response.json();
    if (!response.ok || !["ok", "not found"].includes(result.result)) return false;
    }
    const { error: markError } = await admin.rpc("mark_image_destroyed", { p_id: image.id });
    if (markError) return false;
  }
  const { data: remaining } = await admin.rpc("deletion_batch", { p_job: job.id });
  if (remaining?.images.length) return false;
  if (remaining?.auth_exists) {
    const { error: eraseError } = await admin.auth.admin.deleteUser(job.user_id);
    if (eraseError && eraseError.code !== "user_not_found") return false;
  }
  const { data: done, error: doneError } = await admin.rpc("finish_deletion", { p_job: job.id });
  return !doneError && done === true;
}
Deno.serve(async req => {
  if (req.method !== "POST") return reply({ error: "Method not allowed" }, 405);
  try {
    const reader = req.body?.getReader(); let raw = "";
    if (reader) while (true) { const { value, done } = await reader.read(); if (done) break; raw += new TextDecoder().decode(value); if (raw.length > 4096) { await reader.cancel(); return reply({ error: "Invalid request" }, 400); } }
    const body = JSON.parse(raw || "{}");
    if (typeof body.cron_secret === "string") {
      const { data: secret, error } = await admin.rpc("dispatch_secret");
      const a = new TextEncoder().encode(body.cron_secret), b = new TextEncoder().encode(secret ?? "");
      let diff = a.length ^ b.length; for (let i = 0; i < Math.max(a.length, b.length); i++) diff |= (a[i] ?? 0) ^ (b[i] ?? 0);
      if (error || !b.length || diff) return reply({ error: "Unauthorized" }, 401);
      return reply({ completed: await processJob(null) });
    }
    const jwt = req.headers.get("Authorization")?.replace(/^Bearer /i, "");
    if (!jwt || body.action !== "delete") return reply({ error: "Sign in required" }, 401);
    const { data, error } = await admin.auth.getUser(jwt);
    if (error || !data.user) return reply({ error: "Sign in required" }, 401);
    const user = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, { global: { headers: { Authorization: `Bearer ${jwt}` } }, auth: { persistSession: false } });
    const { data: job, error: beginError } = await user.rpc("begin_deletion");
    if (beginError) return reply({ error: "Sign in again before deleting your account" }, 403);
    // Immediately revoke sessions. The durable job can finish even if this request is interrupted.
    await admin.auth.admin.signOut(jwt, "global");
    const done = await processJob(job).catch(() => false);
    return reply({ deleted: done, pending: !done });
  } catch { return reply({ error: "Account operation unavailable" }, 503); }
});
