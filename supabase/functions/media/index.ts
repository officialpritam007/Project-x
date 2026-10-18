import { inspectAacMp4, inspectJpeg, inspectStoryVideoMp4 } from "./inspect.ts";
import { createClient } from "npm:@supabase/supabase-js@2.52.1";
const url = Deno.env.get("SUPABASE_URL")!;
const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const admin = createClient(url, service, { auth: { persistSession: false } });
const reply = (data: unknown, status = 200) => new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" } });
async function signature(params: Record<string, string>, secret: string) {
  const text = Object.keys(params).sort().map(k => `${k}=${params[k]}`).join("&") + secret;
  return [...new Uint8Array(await crypto.subtle.digest("SHA-1", new TextEncoder().encode(text)))].map(x => x.toString(16).padStart(2, "0")).join("");
}
Deno.serve(async req => {
  if (req.method !== "POST") return reply({ error: "Method not allowed" }, 405);
  const token = req.headers.get("Authorization")?.replace(/^Bearer /i, "");
  if (!token) return reply({ error: "Sign in required" }, 401);
  const { data: auth, error: authError } = await admin.auth.getUser(token);
  if (authError || !auth.user) return reply({ error: "Sign in required" }, 401);
  const cloud = Deno.env.get("CLOUDINARY_CLOUD_NAME"), key = Deno.env.get("CLOUDINARY_API_KEY"), secret = Deno.env.get("CLOUDINARY_API_SECRET");
  const cloudReady = !!(cloud && key && secret);
  const user = createClient(url, Deno.env.get("SUPABASE_ANON_KEY")!, { global: { headers: { Authorization: `Bearer ${token}` } }, auth: { persistSession: false } });
  let uploaded: string | undefined;
  let resourceType = "image";
  let storagePath: string | undefined;
  try {
    if (req.headers.get("Content-Type")?.startsWith("multipart/form-data")) {
      const declared = Number(req.headers.get("Content-Length") ?? "0");
      if (declared > 10585760) return reply({ error: "Image exceeds 10 MB" }, 413);
      // Bound streaming reads as well; Content-Length is not trusted.
      const chunks: Uint8Array[] = []; let bytes = 0;
      const reader = req.body!.getReader();
      while (true) { const { value, done } = await reader.read(); if (done) break; bytes += value.length; if (bytes > 10585760) { await reader.cancel(); return reply({ error: "Image exceeds 10 MB" }, 413); } chunks.push(value); }
      const all = new Uint8Array(bytes); let at = 0; for (const c of chunks) { all.set(c, at); at += c.length; }
      const form = await new Response(all, { headers: { "Content-Type": req.headers.get("Content-Type")! } }).formData();
      const file = form.get("file");
      if (!(file instanceof File) || file.size < 12 || file.size > 10485760) return reply({ error: "Invalid image" }, 400);
      const voice = form.get("kind") === "voice";
      const storyVideo = form.get("kind") === "story_video";
      const cid = form.get("conversation_id");
      if (storyVideo && cid) return reply({ error: "Video stories are not chat attachments" }, 400);
      resourceType = voice || storyVideo ? "video" : "image";
      if (voice && file.size > 2097152) return reply({ error: "Voice exceeds 2 MB" }, 413);
      if (storyVideo && file.size > 8388608) return reply({ error: "Video story exceeds 8 MB" }, 413);
      const magic = new Uint8Array(await file.slice(0, 12).arrayBuffer());
      const jpeg = magic[0] === 255 && magic[1] === 216 && magic[2] === 255;
      const png = magic[0] === 137 && magic[1] === 80 && magic[2] === 78 && magic[3] === 71;
      const webp = String.fromCharCode(...magic.slice(0, 4)) === "RIFF" && String.fromCharCode(...magic.slice(8, 12)) === "WEBP";
      const mp4 = String.fromCharCode(...magic.slice(4, 8)) === "ftyp";
      if (voice || storyVideo ? !mp4 : (!jpeg && !png && !webp)) return reply({ error: "Invalid media format" }, 400);
      const { data: intent, error } = await user.rpc("prepare_upload", {
        p_conversation: cid || null,
        p_kind: storyVideo ? "story_video" : voice ? "voice" : "image",
        // Never route video stories to a paid/unconfigured media provider.
        p_provider: storyVideo || !cloudReady ? "supabase" : "cloudinary"
      });
      if (error) return reply({ error: "Upload unavailable for this account or contact" }, 403);
      if (storyVideo || !cloudReady) {
        const data = new Uint8Array(await file.arrayBuffer());
        let width=0,height=0,duration=0;
        try {
          if(storyVideo)({duration,width,height}=inspectStoryVideoMp4(data));
          else if(voice)duration=inspectAacMp4(data).duration;
          else {const size=inspectJpeg(data);width=size.width;height=size.height;}
        } catch {
          return reply({error:storyVideo?"Use a valid H.264/AAC MP4 video at 720p, no longer than 30s (max 8 MB)":voice?"Use a valid AAC voice message under 120 seconds":"Use a valid JPEG image"},400);
        }
        const format=storyVideo?"mp4":voice?"m4a":"jpg";
        storagePath=intent.public_id+"."+format;
        const {error: providerError}=await admin.rpc("set_media_provider",{p_id:intent.id,p_provider:"supabase"});
        if(providerError)throw new Error("Invalid upload");
        const {error: uploadError}=await admin.storage.from("liquid-media").upload(storagePath,data,{contentType:storyVideo?"video/mp4":voice?"audio/mp4":"image/jpeg",cacheControl:"0",upsert:false});
        if(uploadError)throw new Error("Storage upload failed");
        const finalized=storyVideo
          ? await admin.rpc("finalize_story_video",{p_id:intent.id,p_duration:duration,p_width:width,p_height:height,p_bytes:file.size})
          : voice
          ? await admin.rpc("finalize_voice",{p_id:intent.id,p_format:format,p_duration:duration,p_bytes:file.size})
          : await admin.rpc("finalize_image",{p_id:intent.id,p_format:format,p_width:width,p_height:height,p_bytes:file.size});
        if(finalized.error)throw new Error("Invalid upload");
        storagePath=undefined;
        return reply({id:intent.id,width,height,duration_ms:duration});
      }
      const params = { timestamp: Math.floor(Date.now() / 1000).toString(), public_id: intent.public_id, type: "authenticated", allowed_formats: voice ? "m4a" : "jpg,png,webp", overwrite: "false" };
      const upload = new FormData(); upload.set("file", file);
      for (const [k, v] of Object.entries(params)) upload.set(k, v);
      upload.set("api_key", key!); upload.set("signature", await signature(params, secret!));
      const response = await fetch(`https://api.cloudinary.com/v1_1/${cloud}/${resourceType}/upload`, { method: "POST", body: upload });
      if (!response.ok) return reply({ error: "Upload failed. Try again." }, 502);
      const asset = await response.json(); uploaded = intent.public_id;
      if (asset.public_id !== intent.public_id || asset.type !== "authenticated" || asset.resource_type !== resourceType) throw new Error("Invalid response");
      if (voice && (!asset.audio?.codec || asset.video?.codec || !Number.isFinite(asset.duration) || asset.duration < 0.5 || asset.duration > 120)) throw new Error("Invalid audio");
      const { error: finalizeError } = voice
        ? await admin.rpc("finalize_voice", { p_id: intent.id, p_format: asset.format, p_duration: Math.round(asset.duration * 1000), p_bytes: asset.bytes })
        : await admin.rpc("finalize_image", { p_id: intent.id, p_format: asset.format, p_width: asset.width, p_height: asset.height, p_bytes: asset.bytes });
      if (finalizeError) throw new Error("Invalid upload");
      uploaded = undefined;
      return reply({ id: intent.id, width: asset.width ?? 0, height: asset.height ?? 0, duration_ms: voice ? Math.round(asset.duration * 1000) : 0 });
    }
    const body = await req.json();
    if (body.action !== "download" || typeof body.id !== "string") return reply({ error: "Invalid request" }, 400);
    const { data: image, error } = await user.rpc("image_access", { p_id: body.id });
    if (error || !image) return reply({ error: "Image unavailable" }, 403);
    const expires = image.kind === "voice" ? 180 : image.kind === "story_video" ? 90 : 60;
    if(image.provider==="supabase") {
      const {data,error}=await admin.storage.from("liquid-media").createSignedUrl(image.public_id+"."+image.format,expires);
      if(error||!data?.signedUrl)return reply({error:"Media unavailable"},503);
      return reply({url:data.signedUrl,expires_in:expires,width:image.width,height:image.height});
    }
    if(!cloudReady)return reply({error:"Cloudinary media is temporarily unavailable"},503);
    const params = { timestamp: Math.floor(Date.now() / 1000).toString(), public_id: image.public_id, format: image.format, type: "authenticated", attachment: "false", expires_at: (Math.floor(Date.now() / 1000) + expires).toString() };
    const query = new URLSearchParams({ ...params, api_key: key!, signature: await signature(params, secret!) });
    return reply({ url: `https://api.cloudinary.com/v1_1/${cloud}/${image.resource_type === "video" ? "video" : "image"}/download?${query}`, expires_in: expires, width: image.width, height: image.height });
  } catch {
    if(storagePath)await admin.storage.from("liquid-media").remove([storagePath]).catch(()=>{});
    if (uploaded) {
      const params = { public_id: uploaded, type: "authenticated", timestamp: Math.floor(Date.now() / 1000).toString() };
      const form = new URLSearchParams({ ...params, api_key: key!, signature: await signature(params, secret!) });
      await fetch(`https://api.cloudinary.com/v1_1/${cloud}/${resourceType}/destroy`, { method: "POST", body: form }).catch(() => {});
    }
    return reply({ error: "Image operation failed. Try again." }, 502);
  }
});
