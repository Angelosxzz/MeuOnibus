/** Bound memory use before parsing UTF-8 JSON, including requests without Content-Length. */
export async function readBody(request,limit){
  if(!request.body)return '';
  const reader=request.body.getReader(),chunks=[];let size=0;
  try{
    while(true){
      const {done,value}=await reader.read();if(done)break;
      size+=value.byteLength;
      if(size>limit){reader.cancel().catch(()=>{});return null;}
      chunks.push(value);
    }
    const bytes=new Uint8Array(size);let offset=0;
    for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.byteLength;}
    return new TextDecoder().decode(bytes);
  }finally{reader.releaseLock();}
}
