// Passenger-only gateway. The existing API remains the single source of truth.
const upstream='https://meu-onibus.miguel-angelo09.chatgpt.site';
export default {async fetch(request,env){
 const url=new URL(request.url);
 const allowed=(url.pathname==='/api/schedule'&&request.method==='GET')||(url.pathname==='/api/suggestions/settings'&&request.method==='GET')||(url.pathname==='/api/suggestions'&&request.method==='POST');
 if(url.pathname.startsWith('/api/')){
  if(!allowed)return Response.json({error:'Operação não disponível neste site.'},{status:404});
  const headers=new Headers();
  for(const name of ['content-type','if-none-match'])if(request.headers.has(name))headers.set(name,request.headers.get(name));
  try{
   const result=await fetch(upstream+url.pathname,{method:request.method,headers,body:request.method==='POST'?request.body:undefined,redirect:'error',signal:AbortSignal.timeout(10000)});
   const safe=new Headers({'Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});
   for(const name of ['content-type','etag','retry-after'])if(result.headers.has(name))safe.set(name,result.headers.get(name));
   return new Response(result.body,{status:result.status,headers:safe});
  }catch{return Response.json({error:'Não foi possível conectar ao serviço de horários.'},{status:503});}
 }
 if(['/', '/web'].includes(url.pathname))return Response.redirect(url.origin+'/web/',302);
 if(!['GET','HEAD'].includes(request.method))return new Response('Método não permitido',{status:405});
 return env.ASSETS.fetch(request);
}};
