import {dispatchTokens} from "./dispatch.ts";
function check(value:boolean,message:string){if(!value)throw new Error(message);}
Deno.test("all 49 devices visited despite stale and failed first twelve; concurrency bounded",async()=>{
 const tokens=Array.from({length:49},(_,i)=>`${i}`);let active=0,peak=0;
 const result=await dispatchTokens(tokens,async token=>{
  active++;peak=Math.max(peak,active);await Promise.resolve();active--;
  if(Number(token)<12)throw new Error("stale");return true;
 });
 check(result.attempted.length===49,"silently dropped devices");
 check(result.delivered.length===37,"failed device blocked eligible device");
 check(peak<=12,"unbounded fanout");
});
Deno.test("retry excludes successes and isolates a transient failure",async()=>{
 const first=await dispatchTokens(["a","b","c"],async t=>t!=="b");
 const pending=["a","b","c"].filter(t=>!first.delivered.includes(t));
 const retry=await dispatchTokens(pending,async()=>true);
 check(retry.delivered.join()==="b","repeated successful delivery");
});
Deno.test("deadline reports unattempted devices, never claims completion",async()=>{
 const result=await dispatchTokens(["a","b"],async()=>true,12,Date.now()-1);
 check(result.attempted.length===0&&result.delivered.length===0,"expired invitation dispatched");
});
