/** Every eligible token is visited; failures cannot block the next device. No device limit. */
export async function dispatchTokens(tokens:string[],send:(token:string)=>Promise<boolean>,concurrency=12,deadline=Infinity) {
  const delivered:string[]=[];
  const attempted:string[]=[];
  let cursor=0;
  await Promise.all(Array.from({length:Math.min(concurrency,tokens.length)},async()=>{
    while(cursor<tokens.length&&Date.now()<deadline) {
      const token=tokens[cursor++];attempted.push(token);
      try {if(await send(token))delivered.push(token);}catch { /* isolated per device */ }
    }
  }));
  return {delivered,attempted};
}
