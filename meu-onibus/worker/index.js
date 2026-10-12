import seed from './seed.js';
import {mapPage} from './map-page.js';
import {suggestionsApi} from './suggestions.js';
import {appReleaseApi} from './app-release.js';
import {readBody} from './body.js';
const headers={'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'};
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers});
export function validate(data){
  if(!data||!Array.isArray(data.stops)||data.stops.length!==4||!Array.isArray(data.routes)||data.routes.length!==3)throw Error('São necessárias quatro paradas e três linhas.');
  const stopNames=['Saída','Rodoviária','Pau Serrado','Shopping'],routeNames=['Tabatinga','Umarizeiras','Amanari'];
  const extra=data.additionalStops||[];if(!Array.isArray(extra)||extra.length>100)throw Error('Máximo de 100 paradas adicionais.');const ids=new Set();
  extra.forEach(s=>{if(typeof s.id!=='string'||!s.id||s.id.length>100||ids.has(s.id)||typeof s.name!=='string'||!s.name.trim()||s.name.length>100||!Number.isFinite(s.lat)||Math.abs(s.lat)>90||!Number.isFinite(s.lon)||Math.abs(s.lon)>180||typeof s.confirmed!=='boolean'||!Array.isArray(s.routes)||s.routes.length<1||s.routes.some(r=>!routeNames.includes(r)))throw Error('Parada adicional inválida.');ids.add(s.id);});
  data.stops.forEach((s,i)=>{if(s.name!==stopNames[i]||!Number.isFinite(s.lat)||Math.abs(s.lat)>90||!Number.isFinite(s.lon)||Math.abs(s.lon)>180)throw Error('Coordenadas inválidas.');});
  data.routes.forEach((r,i)=>{if(r.name!==routeNames[i]||typeof r.confirmed!=='boolean'||!Array.isArray(r.trips)||r.trips.length>1000)throw Error('Linha inválida.');
    if(data.schemaVersion===2){
      if(!Array.isArray(r.events)||r.events.length>2000||typeof r.partial!=='boolean'||typeof r.source!=='string'||r.source.length>150||typeof r.note!=='string'||r.note.length>1000)throw Error('Metadados da linha inválidos.');
      for(const value of [r.intervalMinutes,r.averageTravelMinutes])if(!Number.isInteger(value)||value<0||value>1440)throw Error('Média de tempo inválida.');
      if(r.origin&&(!Number.isFinite(r.origin.lat)||Math.abs(r.origin.lat)>90||!Number.isFinite(r.origin.lon)||Math.abs(r.origin.lon)>180||typeof r.origin.confirmed!=='boolean'))throw Error('Origem inválida.');
      r.events.forEach(e=>{if(!Number.isInteger(e.stop)||e.stop<0||e.stop>=4+extra.length||!Number.isInteger(e.minute)||e.minute<0||e.minute>1439||typeof e.destination!=='string'||!e.destination.trim()||e.destination.length>100||typeof e.vehicle!=='string'||e.vehicle.length>100||!['tabela','estimado'].includes(e.kind))throw Error('Passagem ou sentido inválido.');});
    }
    r.trips.forEach(t=>{if(!Array.isArray(t)||t.length!==5||!Number.isInteger(t[0])||t[0]<0||t[0]>1439)throw Error('Saída inválida.');let last=t[0];for(let k=1;k<5;k++){const v=t[k];if(v===null)continue;if(!Number.isInteger(v)||v<last||v>1439)throw Error('Horários devem seguir a ordem de passagem, de 00:00 a 23:59.');last=v;}});
  });return data;
}
async function equalSecret(a,b){if(!a||!b)return false;const hash=async value=>new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value)));const [x,y]=await Promise.all([hash(a),hash(b)]);let different=0;for(let i=0;i<x.length;i++)different|=x[i]^y[i];return different===0;}
export default {async fetch(request,env){
  const path=new URL(request.url).pathname;
  try{
    const releaseResponse=appReleaseApi(request);if(releaseResponse)return releaseResponse;
    if(path==='/api/health')return json({service:'Meu Ônibus',status:'ok'});
    if(path.startsWith('/api/suggestions'))return await suggestionsApi(request,env,seed,validate,equalSecret,json);
    if(path==='/map-picker'&&request.method==='GET'){
      const nonce=crypto.randomUUID();
      return new Response(mapPage(nonce),{headers:{'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store','Referrer-Policy':'strict-origin-when-cross-origin','X-Content-Type-Options':'nosniff','Content-Security-Policy':`default-src 'none'; script-src 'nonce-${nonce}' https://unpkg.com; style-src 'unsafe-inline' https://unpkg.com; img-src data: https://tile.openstreetmap.org https://unpkg.com; connect-src https://tile.openstreetmap.org; frame-src 'none'; frame-ancestors 'none'; object-src 'none'; base-uri 'none'; form-action 'none'`}});
    }
    if(path!=='/api/schedule')return json({service:'API do aplicativo Android Meu Ônibus',endpoint:'/api/schedule'},404);
    if(!env.DB)return json({error:'Banco de dados indisponível.'},503);
    if(request.method==='GET'){
      const row=await env.DB.prepare('SELECT payload, revision, updated_at FROM schedules WHERE id=1').first();
      const etag='"schedule-'+(row?.revision||0)+'"',responseHeaders={...headers,ETag:etag};
      const condition=request.headers.get('If-None-Match');
      if(condition&&(condition.trim()==='*'||condition.split(',').some(value=>value.trim().replace(/^W\//,'')===etag)))return new Response(null,{status:304,headers:responseHeaders});
      return new Response(JSON.stringify(row?{data:JSON.parse(row.payload),revision:row.revision,updatedAt:row.updated_at}:{data:seed,revision:0,updatedAt:null}),{headers:responseHeaders});
    }
    if(request.method!=='PUT')return json({error:'Método não permitido.'},405);
    if(!await equalSecret(request.headers.get('Authorization')?.replace(/^Bearer /,''),env.ADMIN_KEY))return json({error:'Chave de administrador inválida.'},401);
    if(Number(request.headers.get('Content-Length')||0)>200000)return json({error:'Tabela muito grande.'},413);
    const raw=await readBody(request,200000);if(raw===null)return json({error:'Tabela muito grande.'},413);
    let body;try{body=JSON.parse(raw);validate(body.data);}catch(e){return json({error:e.message||'Dados inválidos.'},400);}
    if(!Number.isInteger(body.revision)||body.revision<0)return json({error:'Versão dos dados inválida.'},400);
    const updatedAt=new Date().toISOString();
    const row=await env.DB.prepare('INSERT INTO schedules(id,payload,revision,updated_at) SELECT 1,?,1,? WHERE ?=0 ON CONFLICT(id) DO UPDATE SET payload=excluded.payload, revision=schedules.revision+1, updated_at=excluded.updated_at WHERE schedules.revision=? RETURNING revision').bind(JSON.stringify(body.data),updatedAt,body.revision,body.revision).first();
    // Existing revision>0 needs an UPDATE because INSERT SELECT above intentionally only creates revision zero.
    let revision=row?.revision;
    if(!revision&&body.revision>0){const updated=await env.DB.prepare('UPDATE schedules SET payload=?,revision=revision+1,updated_at=? WHERE id=1 AND revision=? RETURNING revision').bind(JSON.stringify(body.data),updatedAt,body.revision).first();revision=updated?.revision;}
    if(!revision)return json({error:'Os horários foram alterados em outro aparelho. Atualize e tente novamente.'},409);
    return json({data:body.data,revision,updatedAt});
  }catch{return json({error:'Não foi possível acessar os horários. Tente novamente.'},500);}
}};
