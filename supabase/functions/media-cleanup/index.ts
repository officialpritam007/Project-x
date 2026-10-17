import { createClient } from "npm:@supabase/supabase-js@2.49.4";
const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false } });
const reply = (data: unknown, status = 200) => new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" } });
Deno.serve(async req => {
  if (req.method !== "POST") return reply({ error: "Method not allowed" }, 405);
  try {
    const reader = req.body?.getReader(); let text = "";
    if (reader) while (true) { const { value, done } = await reader.read(); if (done) break; text += new TextDecoder().decode(value); if (text.length > 4096) { await reader.cancel(); return reply({ error: "Invalid request" }, 400); } }
    const body = JSON.parse(text);
    if (typeof body.cron_secret !== "string") return reply({ error: "Unauthorized" }, 401);
    const { data: expected, error } = await admin.rpc("dispatch_secret");
    const a = new TextEncoder().encode(body.cron_secret), b = new TextEncoder().encode(expected ?? "");
    let diff = a.length ^ b.length; for (let i = 0; i < Math.max(a.length, b.length); i++) diff |= (a[i] ?? 0) ^ (b[i] ?? 0);
    if (error || !b.length || diff) return reply({ error: "Unauthorized" }, 401);
    const cloud = Deno.env.get("CLOUDINARY_CLOUD_NAME"), key = Deno.env.get("CLOUDINARY_API_KEY"), secret = Deno.env.get("CLOUDINARY_API_SECRET");
    if (!cloud || !key || !secret) return reply({ error: "Media cleanup unavailable" }, 503);
    const { data: jobs, error: claimError } = await admin.rpc("garbage_images");
    if (claimError) return reply({ error: "Cleanup unavailable" }, 503);
    let removed = 0;
    for (const job of jobs) {
      const params = { public_id: job.public_id, type: "authenticated", invalidate: "true", timestamp: Math.floor(Date.now() / 1000).toString() };
      const input = Object.keys(params).sort().map(k => `${k}=${params[k as keyof typeof params]}`).join("&") + secret;
      const signature = [...new Uint8Array(await crypto.subtle.digest("SHA-1", new TextEncoder().encode(input)))].map(x => x.toString(16).padStart(2, "0")).join("");
      const response = await fetch(`https://api.cloudinary.com/v1_1/${cloud}/${job.resource_type === "video" ? "video" : "image"}/destroy`, { method: "POST", body: new URLSearchParams({ ...params, api_key: key, signature }), signal: AbortSignal.timeout(8000) });
      const result = await response.json();
      if (response.ok && ["ok", "not found"].includes(result.result)) { await admin.rpc("finish_garbage", { p_id: job.id }); removed++; }
    }
    return reply({ removed });
  } catch { return reply({ error: "Cleanup unavailable" }, 503); }
});
