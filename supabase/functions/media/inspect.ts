/** Validate a bounded still RGBA PNG for one-to-one stickers. */
export function inspectStickerPng(data:Uint8Array):{width:number;height:number} {
  const signature=[137,80,78,71,13,10,26,10];
  if(data.length<100||data.length>524288||
      signature.some((part,i)=>data[i]!==part))throw new Error("Use a PNG under 512 KB");
  const view=new DataView(data.buffer,data.byteOffset,data.byteLength);
  let cursor=8,seenIHDR=false,seenIDAT=false,seenIEND=false,count=0;
  while(cursor<data.length) {
    if(++count>1000 || cursor+12>data.length)throw new Error("Invalid PNG chunks");
    const size=view.getUint32(cursor);
    if(size>524288 || size+12>data.length-cursor)throw new Error("Invalid PNG length");
    const type=String.fromCharCode(...data.subarray(cursor+4,cursor+8));
    const chunk=cursor+8;
    if(!seenIHDR) {
      if(type!=="IHDR"||size!==13)throw new Error("Missing PNG header");
      if(view.getUint32(chunk)!==512||view.getUint32(chunk+4)!==512 ||
          data[chunk+8]!==8||data[chunk+9]!==6 ||
          data[chunk+10]!==0||data[chunk+11]!==0||data[chunk+12]!==0)
        throw new Error("Sticker must be 512x512 RGBA PNG");
      seenIHDR=true;
    } else if(type==="IHDR"||type==="acTL"||type==="fcTL"||type==="fdAT") {
      throw new Error("Animated PNG is unsupported");
    } else if(type==="IDAT") {
      if(size===0 || seenIEND)throw new Error("Invalid sticker pixels");
      seenIDAT=true;
    } else if(type==="IEND") {
      if(size!==0||!seenIDAT||cursor+12!==data.length)throw new Error("Invalid PNG ending");
      seenIEND=true;
    } else if(type[0]>="A" && type[0]<="Z") {
      throw new Error("Unsupported PNG chunk");
    }
    // Check CRC32 over the chunk type and payload to reject corrupted PNGs.
    let crc=0xffffffff;
    for(let i=cursor+4;i<cursor+8+size;i++) {
      crc^=data[i];
      for(let b=0;b<8;b++)crc=(crc&1)!==0 ? ((crc>>>1)^0xedb88320)>>>0 : crc>>>1;
    }
    if(((crc^0xffffffff)>>>0)!==view.getUint32(cursor+8+size))throw new Error("Bad PNG checksum");
    cursor+=size+12;
    if(seenIEND)break;
  }
  if(!seenIHDR||!seenIDAT||!seenIEND||cursor!==data.length)throw new Error("Incomplete PNG");
  return {width:512,height:512};
}

/** Bounded container inspection for the private-storage fallback. No executable media types. */
export function inspectJpeg(data: Uint8Array): {width:number;height:number} {
  if(data.length<12||data[0]!==255||data[1]!==216||data[data.length-2]!==255||data[data.length-1]!==217)throw new Error("Use JPEG");
  const view=new DataView(data.buffer,data.byteOffset,data.byteLength);let cursor=2;
  while(cursor+4<data.length) {
    if(data[cursor++]!==255)throw new Error("Invalid JPEG");
    while(cursor<data.length&&data[cursor]===255)cursor++;
    const marker=data[cursor++];if(marker===218||marker===217)break;
    if(marker===1||(marker>=208&&marker<=215))continue;
    if(cursor+2>data.length)break;
    const length=view.getUint16(cursor);if(length<2||cursor+length>data.length)throw new Error("Invalid JPEG");
    if([192,193,194,195,197,198,199,201,202,203,205,206,207].includes(marker)) {
      if(length<8)throw new Error("Invalid JPEG");
      const height=view.getUint16(cursor+3),width=view.getUint16(cursor+5);
      if(width<1||height<1||width*height>25000000)throw new Error("Image too large");
      return {width,height};
    }
    cursor+=length;
  }
  throw new Error("Invalid JPEG");
}
type Box={type:string;start:number;end:number};
export function inspectAacMp4(data:Uint8Array):{duration:number} {
  if(data.length<32||data.length>2097152)throw new Error("Invalid audio");
  const view=new DataView(data.buffer,data.byteOffset,data.byteLength);
  const text=(at:number)=>String.fromCharCode(...data.slice(at,at+4));
  let count=0;
  const boxes=(start:number,end:number):Box[]=>{
    const result:Box[]=[];let cursor=start;
    while(cursor<end){
      if(++count>10000||end-cursor<8)throw new Error("Invalid MP4");
      let length=view.getUint32(cursor),header=8;
      if(length===1){if(end-cursor<16)throw new Error("Invalid MP4");const large=view.getBigUint64(cursor+8);if(large>BigInt(end-cursor))throw new Error("Invalid MP4");length=Number(large);header=16;}
      if(length===0)length=end-cursor;
      if(length<header||cursor+length>end)throw new Error("Invalid MP4");
      result.push({type:text(cursor+4),start:cursor+header,end:cursor+length});cursor+=length;
    }
    return result;
  };
  const children=(box:Box)=>boxes(box.start,box.end);
  const required=(list:Box[],type:string)=>{const found=list.filter(x=>x.type===type);if(found.length!==1)throw new Error("Invalid MP4");return found[0];};
  const top=boxes(0,data.length);required(top,"ftyp");const moov=required(top,"moov");
  if(!top.some(x=>x.type==="mdat"&&x.end>x.start))throw new Error("No audio samples");
  const tracks=children(moov).filter(x=>x.type==="trak");if(tracks.length!==1)throw new Error("Audio only");
  const mdia=required(children(tracks[0]),"mdia"),parts=children(mdia),handler=required(parts,"hdlr");
  if(handler.end-handler.start<12||text(handler.start+8)!=="soun")throw new Error("Audio only");
  const mdhd=required(parts,"mdhd");if(mdhd.end-mdhd.start<24)throw new Error("Invalid duration");
  const version=data[mdhd.start];let scale:number,samples:number;
  if(version===0){scale=view.getUint32(mdhd.start+12);samples=view.getUint32(mdhd.start+16);}
  else if(version===1){if(mdhd.end-mdhd.start<36)throw new Error("Invalid duration");scale=view.getUint32(mdhd.start+20);const big=view.getBigUint64(mdhd.start+24);if(big>BigInt(Number.MAX_SAFE_INTEGER))throw new Error("Invalid duration");samples=Number(big);}
  else throw new Error("Invalid duration");
  const duration=Math.round(samples/scale*1000);if(!Number.isFinite(duration)||scale===0||duration<500||duration>120000)throw new Error("Use 0.5–120 seconds");
  const minf=required(parts,"minf"),stbl=required(children(minf),"stbl"),stsd=required(children(stbl),"stsd");
  if(stsd.end-stsd.start<16||view.getUint32(stsd.start+4)!==1)throw new Error("Invalid audio codec");
  const entry=required(boxes(stsd.start+8,stsd.end),"mp4a");
  if(entry.end-entry.start<28)throw new Error("Invalid AAC");
  return {duration};
}


/**
 * Inspect an untrusted, bounded, *already-transcoded* MP4 video story before storage.
 * Requires H.264 video, optional AAC audio, <=30 s, <=1280x720 in either orientation.
 * The client still performs trim/transcoding; this is the server enforcement boundary.
 */
export function inspectStoryVideoMp4(data:Uint8Array):{duration:number;width:number;height:number} {
  if(data.length<64||data.length>8*1024*1024)throw new Error("Video exceeds 8 MB");
  const view=new DataView(data.buffer,data.byteOffset,data.byteLength);
  const name=(p:number)=>String.fromCharCode(...data.subarray(p,p+4));
  type Atom={type:string;start:number;end:number};
  let budget=0;
  const atoms=(start:number,end:number):Atom[]=>{
    const out:Atom[]=[];
    let p=start;
    while(p<end) {
      if(++budget>10000||p+8>end)throw new Error("Invalid MP4 atoms");
      let len=view.getUint32(p),head=8;
      if(len===1) {
        if(p+16>end)throw new Error("Invalid atom");
        const huge=view.getBigUint64(p+8);
        if(huge>BigInt(end-p))throw new Error("Invalid atom");
        len=Number(huge);head=16;
      }
      if(len===0)len=end-p;
      if(len<head||p+len>end)throw new Error("Truncated atom");
      out.push({type:name(p+4),start:p+head,end:p+len});
      p+=len;
    }
    return out;
  };
  const required=(list:Atom[],type:string)=>{
    const found=list.filter(x=>x.type===type);
    if(found.length!==1)throw new Error("Missing or duplicate "+type);
    return found[0];
  };
  const top=atoms(0,data.length);
  required(top,"ftyp");
  const moov=required(top,"moov");
  if(!top.some(x=>x.type==="mdat"&&x.end-x.start>64))throw new Error("Missing video samples");
  const tracks=atoms(moov.start,moov.end).filter(x=>x.type==="trak");
  if(tracks.length<1||tracks.length>2)throw new Error("Unexpected tracks");
  let duration=0,width=0,height=0,videoCount=0,audioCount=0;
  for(const tr of tracks) {
    const parts=atoms(tr.start,tr.end);
    const tkhd=required(parts,"tkhd");
    if(tkhd.end-tkhd.start<8)throw new Error("Invalid dimensions");
    const mdia=required(parts,"mdia"),m=atoms(mdia.start,mdia.end);
    const hdlr=required(m,"hdlr");
    if(hdlr.end-hdlr.start<12)throw new Error("Invalid handler");
    const handler=name(hdlr.start+8);
    if(handler!=="vide"&&handler!=="soun")throw new Error("Unexpected stream");
    const mdhd=required(m,"mdhd");
    let scale=0,samples=0;
    if(data[mdhd.start]===0) {
      if(mdhd.end-mdhd.start<20)throw new Error("Invalid duration");
      scale=view.getUint32(mdhd.start+12);
      samples=view.getUint32(mdhd.start+16);
    } else if(data[mdhd.start]===1) {
      if(mdhd.end-mdhd.start<32)throw new Error("Invalid duration");
      scale=view.getUint32(mdhd.start+20);
      const big=view.getBigUint64(mdhd.start+24);
      if(big>BigInt(Number.MAX_SAFE_INTEGER))throw new Error("Invalid duration");
      samples=Number(big);
    } else throw new Error("Invalid duration version");
    const milliseconds=Math.round(samples/scale*1000);
    // AAC encoders can add priming/padding samples after the last video frame.
    // Enforce the exact 30s limit on video frames, with only 1s container padding
    // tolerated for AAC. The persisted story duration comes from the video track.
    const maximum=handler==="vide"?30000:31000;
    if(!scale||!Number.isFinite(milliseconds)||milliseconds<500||milliseconds>maximum)
      throw new Error("Video must be 0.5–30 seconds");
    const minf=required(m,"minf"),stbl=required(atoms(minf.start,minf.end),"stbl");
    const stsd=required(atoms(stbl.start,stbl.end),"stsd");
    if(stsd.end-stsd.start<16||view.getUint32(stsd.start+4)!==1)
      throw new Error("Invalid sample description");
    const entry=atoms(stsd.start+8,stsd.end);
    if(entry.length!==1)throw new Error("Invalid codec");
    if(handler==="vide") {
      videoCount++;
      if(!["avc1","avc3"].includes(entry[0].type))throw new Error("H.264 required");
      width=view.getUint32(tkhd.end-8)>>>16;
      height=view.getUint32(tkhd.end-4)>>>16;
      if(!width||!height||Math.min(width,height)>720||Math.max(width,height)>1280)
        throw new Error("Video exceeds 720p");
      duration=milliseconds;
    } else {
      audioCount++;
      if(entry[0].type!=="mp4a")throw new Error("AAC required");
    }
  }
  if(videoCount!==1||audioCount>1)throw new Error("One video track required");
  return {duration,width,height};
}
