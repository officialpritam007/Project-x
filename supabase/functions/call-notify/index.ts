import { createClient } from "npm:@supabase/supabase-js@2.52.1";

const url = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const admin = createClient(url, serviceKey, { auth: { persistSession: false } });
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" } });
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const base64url = (bytes: Uint8Array) =>
  btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");

async function firebaseToken(credentials: { client_email: string; private_key: string }) {
  const now = Math.floor(Date.now()/1000);
  const encode=(value: unknown)=>base64url(new TextEncoder().encode(JSON.stringify(value)));
  const unsigned=`${encode({alg:"RS256",typ:"JWT"})}.${encode({iss:credentials.client_email,scope:"https://www.googleapis.com/auth/firebase.messaging",aud:"https://oauth2.googleapis.com/token",iat:now,exp:now+3600})}`;
  const keyData=credentials.private_key.replace(/-----[^-]+-----/g, "").replace(/\s/g, "");
  const key=await crypto.subtle.importKey("pkcs8",Uint8Array.from(atob(keyData),c=>c.charCodeAt(0)),{name:"RSASSA-PKCS1-v1_5",hash:"SHA-256"},false,["sign"]);
  const signed=base64url(new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5",key,new TextEncoder().encode(unsigned))));
  const response=await fetch("https://oauth2.googleapis.com/token",{
    method:"POST",body:new URLSearchParams({grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",assertion:`${unsigned}.${signed}`}),
    signal:AbortSignal.timeout(8000)
  });
  if(!response.ok)throw new Error("FCM auth unavailable");
  const body=await response.json();
  if(typeof body.access_token!=="string")throw new Error("FCM auth unavailable");
  return body.access_token as string;
}

Deno.serve(async (req:Request)=>{
  if(req.method!=="POST")return json({error:"Method not allowed"},405);
  try {
    const header=req.headers.get("Authorization")||"";
    if(!header.startsWith("Bearer "))return json({error:"Sign in required"},401);
    const jwt=header.slice(7);
    const {data:{user},error:authError}=await admin.auth.getUser(jwt);
    if(authError||!user)return json({error:"Sign in required"},401);
    const reader=req.body?.getReader();
    const decoder=new TextDecoder();
    let raw="";
    if(reader)while(true){
      const {value,done}=await reader.read();
      if(done)break;
      raw+=decoder.decode(value,{stream:true});
      if(raw.length>512){
        await reader.cancel();
        return json({error:"Invalid request"},413);
      }
    }
    raw+=decoder.decode();
    if(raw.length>512)return json({error:"Invalid request"},413);
    const input=JSON.parse(raw||"{}");
    if(typeof input.callId!=="string"||!uuidPattern.test(input.callId))
      return json({error:"Invalid call ID"},400);

    // Do not mark an invitation dispatched when credentials are missing.
    const secret=Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");
    if(!secret)return json({error:"Push not configured"},503);
    const credentials=JSON.parse(secret);
    if(typeof credentials.project_id!=="string"||!/^[a-z0-9-]+$/.test(credentials.project_id))
      throw new Error("Invalid FCM configuration");
    const token=await firebaseToken(credentials);

    // DB locks the call, checks owner/state/expiry/block and claims one push atomically.
    const {data:claimed,error}=await admin.rpc("claim_call_push",{p_call:input.callId,p_caller:user.id});
    if(error)throw new Error("Call push unavailable");
    if(!claimed)return json({accepted:false},202);
    const recipients:Array<string>=Array.isArray(claimed.tokens)?claimed.tokens:[];
    const delivered:string[]=[];
    // Bound concurrency, not recipient coverage: one claim can contain more than
    // twelve registered devices. Process every token in small parallel batches.
    for(let offset=0;offset<recipients.length;offset+=12) {
      await Promise.all(recipients.slice(offset,offset+12).map(async (device:string)=>{
      if(typeof device!=="string"||device.length>4096)return;
      try {
        const response=await fetch(`https://fcm.googleapis.com/v1/projects/${credentials.project_id}/messages:send`,{
          method:"POST",headers:{Authorization:`Bearer ${token}`,"Content-Type":"application/json"},
          body:JSON.stringify({message:{token:device,data:{
            event:"incoming_call",
            callId:String(claimed.callId),
            recipientId:String(claimed.recipientId),
            conversationId:String(claimed.conversationId),
            kind:String(claimed.kind)
          },android:{priority:"high",ttl:"45s",collapse_key:`call-${claimed.recipientId}`}}}),
          signal:AbortSignal.timeout(8000)
        });
        if(response.ok)delivered.push(device);
        else {
          const failure=await response.json().catch(()=>({}));
          if(failure?.error?.details?.some((x:{errorCode?:string})=>x.errorCode==="UNREGISTERED"))
            await admin.rpc("remove_invalid_device",{p_token:device,p_recipient:claimed.recipientId});
        }
      }catch { /* Do not leak FCM credentials or tokens to callers. */ }
      }));
    }
    const {error:finishError}=await admin.rpc("finish_call_push",{p_call:input.callId,p_delivered:delivered});
    if(finishError)throw new Error("Dispatch completion unavailable");
    const complete=delivered.length===recipients.length;
    return json({accepted:true,complete,attempted:recipients.length,delivered:delivered.length},complete?200:503);
  }catch {
    return json({error:"Call push temporarily unavailable"},503);
  }
});
