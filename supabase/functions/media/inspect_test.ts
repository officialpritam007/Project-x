import {inspectAacMp4,inspectJpeg} from "./inspect.ts";
const assert=(ok:boolean)=>{if(!ok)throw new Error("Assertion failed");};
const bad=(fn:()=>unknown)=>{let failed=false;try{fn();}catch{failed=true;}assert(failed);};
const join=(...parts:Uint8Array[])=>{const out=new Uint8Array(parts.reduce((n,p)=>n+p.length,0));let at=0;for(const p of parts){out.set(p,at);at+=p.length;}return out;};
const box=(type:string,...parts:Uint8Array[])=>{const data=join(...parts);const out=new Uint8Array(data.length+8);new DataView(out.buffer).setUint32(0,out.length);out.set(new TextEncoder().encode(type),4);out.set(data,8);return out;};
function audio(handler="soun",duration=48000):Uint8Array {
 const mdhd=new Uint8Array(24);new DataView(mdhd.buffer).setUint32(12,48000);new DataView(mdhd.buffer).setUint32(16,duration);
 const hdlr=new Uint8Array(12);hdlr.set(new TextEncoder().encode(handler),8);
 const count=new Uint8Array(8);new DataView(count.buffer).setUint32(4,1);
 return join(box("ftyp",new TextEncoder().encode("M4A ")),box("moov",box("trak",box("mdia",box("mdhd",mdhd),box("hdlr",hdlr),box("minf",box("stbl",box("stsd",count,box("mp4a",new Uint8Array(28)))))))),box("mdat",new Uint8Array([1,2,3])));
}
Deno.test("AAC metadata accepts bounded audio and rejects video, truncation and duration abuse",()=>{
 assert(inspectAacMp4(audio()).duration===1000);
 bad(()=>inspectAacMp4(audio("vide")));bad(()=>inspectAacMp4(audio("soun",48000*121)));
 bad(()=>inspectAacMp4(audio().slice(0,-1)));bad(()=>inspectAacMp4(new Uint8Array(2097153)));
});
Deno.test("JPEG dimensions reject oversized frames and truncated containers",()=>{
 const data=new Uint8Array([255,216,255,192,0,8,8,0,40,0,32,0,255,217]);
 const size=inspectJpeg(data);assert(size.width===32&&size.height===40);
 bad(()=>inspectJpeg(data.slice(0,-1)));
 const huge=data.slice();huge[7]=255;huge[8]=255;huge[9]=255;huge[10]=255;bad(()=>inspectJpeg(huge));
});
