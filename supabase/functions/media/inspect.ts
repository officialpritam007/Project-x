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
