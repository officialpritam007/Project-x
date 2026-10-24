import {createClient} from "npm:@supabase/supabase-js@2.52.1";

const json=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:{"Content-Type":"application/json","Cache-Control":"no-store"}});
Deno.serve(async(req:Request)=>{
  if(req.method!=="POST")return json({error:"Method not allowed"},405);
  try {
    const authorization=req.headers.get("Authorization")||"";
    if(!authorization.startsWith("Bearer "))return json({error:"Sign in required"},401);
    const client=createClient(Deno.env.get("SUPABASE_URL")!,Deno.env.get("SUPABASE_ANON_KEY")!,{
      global:{headers:{Authorization:authorization}},auth:{persistSession:false}});
    const {data:{user},error}=await client.auth.getUser();
    if(error||!user)return json({error:"Sign in required"},401);
    // Bound streamed input, including chunked requests.
    const reader=req.body?.getReader();let size=0;const chunks:Uint8Array[]=[];
    if(reader)while(true){const {value,done}=await reader.read();if(done)break;size+=value.length;if(size>512){await reader.cancel();return json({error:"Invalid request"},413);}chunks.push(value);}
    const bytes=new Uint8Array(size);let offset=0;for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.length;}
    const input=JSON.parse(new TextDecoder().decode(bytes));
    if(typeof input.conversationId!=="string"||!/^[0-9a-f-]{36}$/i.test(input.conversationId))return json({error:"Invalid conversation"},400);
    const allowed=await client.rpc("call_ice_authorize",{p_conversation:input.conversationId});
    if(allowed.error||allowed.data!==true)return json({error:"Call unavailable"},403);
    // Coturn REST credentials. The shared secret exists only in server environment secrets.
    const secret=Deno.env.get("TURN_REST_SECRET");
    const urls=(Deno.env.get("TURN_URLS")||"").split(",").map(x=>x.trim()).filter(Boolean);
    if(!secret||!urls.length||urls.some(x=>!/^turns?:[^\s]+$/.test(x)))return json({error:"TURN not configured"},503);
    const expiresAt=Math.floor(Date.now()/1000)+600;
    const username=`${expiresAt}:${user.id}`;
    const key=await crypto.subtle.importKey("raw",new TextEncoder().encode(secret),{name:"HMAC",hash:"SHA-1"},false,["sign"]);
    const signature=new Uint8Array(await crypto.subtle.sign("HMAC",key,new TextEncoder().encode(username)));
    return json({urls,username,credential:btoa(String.fromCharCode(...signature)),expiresAt});
  }catch {return json({error:"TURN temporarily unavailable"},503);}
});
