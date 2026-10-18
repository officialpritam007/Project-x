import {inspectAacMp4,inspectJpeg,inspectStoryVideoMp4} from "./inspect.ts";
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


function storyVideo(duration=30000,width=720,height=1280,codec="avc1",handler="vide"):Uint8Array {
 const tkhd=new Uint8Array(84);
 const tk=new DataView(tkhd.buffer);tk.setUint32(tkhd.length-8,width<<16);tk.setUint32(tkhd.length-4,height<<16);
 const mdhd=new Uint8Array(24);const mv=new DataView(mdhd.buffer);mv.setUint32(12,1000);mv.setUint32(16,duration);
 const hdlr=new Uint8Array(12);hdlr.set(new TextEncoder().encode(handler),8);
 const stsd=new Uint8Array(8);new DataView(stsd.buffer).setUint32(4,1);
 return join(
   box("ftyp",new TextEncoder().encode("isom")),
   box("moov",box("trak",box("tkhd",tkhd),
     box("mdia",box("mdhd",mdhd),box("hdlr",hdlr),
       box("minf",box("stbl",box("stsd",stsd,box(codec,new Uint8Array(32)))))))),
   box("mdat",new Uint8Array(128))
 );
}
Deno.test("Video story validation accepts 30-second H264 720p and rejects abuse",()=>{
 const parsed=inspectStoryVideoMp4(storyVideo());
 assert(parsed.duration===30000&&parsed.width===720&&parsed.height===1280);
 bad(()=>inspectStoryVideoMp4(storyVideo(30001)));
 bad(()=>inspectStoryVideoMp4(storyVideo(1000,1080,1920)));
 bad(()=>inspectStoryVideoMp4(storyVideo(1000,720,1280,"hev1")));
 bad(()=>inspectStoryVideoMp4(storyVideo(1000,720,1280,"avc1","soun")));
 bad(()=>inspectStoryVideoMp4(storyVideo().slice(0,-3)));
 bad(()=>inspectStoryVideoMp4(new Uint8Array(8*1024*1024+1)));
});


function withPaddedAudio(audioDurationMs:number):Uint8Array {
 const source=storyVideo();
 const view=new DataView(source.buffer);
 const moovAt=view.getUint32(0);
 const moovLength=view.getUint32(moovAt);
 const mdhd=new Uint8Array(24);
 new DataView(mdhd.buffer).setUint32(12,1000);
 new DataView(mdhd.buffer).setUint32(16,audioDurationMs);
 const hdlr=new Uint8Array(12);
 hdlr.set(new TextEncoder().encode("soun"),8);
 const stsd=new Uint8Array(8);
 new DataView(stsd.buffer).setUint32(4,1);
 const audioTrack=box("trak",box("tkhd",new Uint8Array(84)),
    box("mdia",box("mdhd",mdhd),box("hdlr",hdrCorrect()),
      box("minf",box("stbl",box("stsd",stsd,box("mp4a",new Uint8Array(32)))))));
 function hdrCorrect():Uint8Array{return hdlr;}
 const newMoov=box("moov",source.slice(moovAt+8,moovAt+moovLength),audioTrack);
 return join(source.slice(0,moovAt),newMoov,source.slice(moovAt+moovLength));
}
Deno.test("AAC priming padding does not reject an exact 30-second story",()=>{
 assert(inspectStoryVideoMp4(withPaddedAudio(30400)).duration===30000);
 bad(()=>inspectStoryVideoMp4(withPaddedAudio(31001)));
});
