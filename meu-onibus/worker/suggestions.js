const clean=value=>typeof value==='string'?value.trim():'';
const number=value=>typeof value==='number'&&Number.isFinite(value);
function target(data,id){if(id.startsWith('origin:'))return data.routes.find(r=>'origin:'+r.name===id)?.origin;if(id.startsWith('stop:'))return data.stops.find(s=>'stop:'+s.name===id);if(id.startsWith('extra:'))return (data.additionalStops||[]).find(s=>'extra:'+s.id===id);return null;}
export function validateSuggestion(body,data){
  if(!body||!['update','add'].includes(body.operation))throw Error('Escolha alterar uma parada ou sugerir uma nova.');
  const proposal={operation:body.operation,target:clean(body.target),name:clean(body.name),route:clean(body.route),reason:clean(body.reason),lat:body.lat??null,lon:body.lon??null};
  if(proposal.name.length>100||proposal.reason.length>1000)throw Error('Nome ou observação muito longa.');
  const location=proposal.lat!==null||proposal.lon!==null;
  if(location&&(!number(proposal.lat)||Math.abs(proposal.lat)>90||!number(proposal.lon)||Math.abs(proposal.lon)>180))throw Error('Selecione uma localização válida no mapa.');
  if(proposal.operation==='update'){if(!target(data,proposal.target))throw Error('Parada não encontrada.');if(!proposal.name&&!location)throw Error('Informe um novo nome ou selecione uma localização.');}
  else{if(!proposal.name||!location||!data.routes.some(r=>r.name===proposal.route))throw Error('Nova parada exige nome, linha e ponto no mapa.');proposal.target='';}
  return proposal;
}
export function applySuggestion(data,proposal,id){
  const result=structuredClone(data);
  if(proposal.operation==='add'){result.additionalStops||=[];result.additionalStops.push({id:'s-'+id,name:proposal.name,lat:proposal.lat,lon:proposal.lon,confirmed:true,routes:[proposal.route]});}
  else{const point=target(result,proposal.target);if(!point)throw Error('A parada não existe mais.');if(proposal.name){if(proposal.target.startsWith('extra:'))point.name=proposal.name;else point.displayName=proposal.name;}if(proposal.lat!==null){point.lat=proposal.lat;point.lon=proposal.lon;point.confirmed=true;}}
  return result;
}
export async function suggestionsApi(request,env,seed,validate,authorize,json){
  const path=new URL(request.url).pathname;
  if(!env.DB)return json({error:'Banco indisponível.'},503);
  const setting=await env.DB.prepare('SELECT enabled FROM suggestion_settings WHERE id=1').first();const accepting=setting?setting.enabled===1:true;
  if(path==='/api/suggestions/settings'&&request.method==='GET')return json({accepting});
  if(path==='/api/suggestions/settings'&&request.method==='PUT'){
    if(!await authorize(request.headers.get('Authorization')?.replace(/^Bearer /,''),env.ADMIN_KEY))return json({error:'Chave de administrador inválida.'},401);
    const raw=await request.text();if(raw.length>200)return json({error:'Configuração inválida.'},400);let body;try{body=JSON.parse(raw);}catch{return json({error:'Configuração inválida.'},400);}if(typeof body.accepting!=='boolean')return json({error:'Informe se deseja receber sugestões.'},400);
    await env.DB.prepare('INSERT INTO suggestion_settings(id,enabled,updated_at) VALUES(1,?,?) ON CONFLICT(id) DO UPDATE SET enabled=excluded.enabled,updated_at=excluded.updated_at').bind(body.accepting?1:0,new Date().toISOString()).run();return json({accepting:body.accepting});
  }
  const row=await env.DB.prepare('SELECT payload,revision FROM schedules WHERE id=1').first();const data=row?JSON.parse(row.payload):seed;
  if(path==='/api/suggestions'&&request.method==='POST'){
    if(!accepting)return json({error:'O administrador pausou o recebimento de sugestões. Tente novamente mais tarde.'},403);
    const raw=await request.text();if(raw.length>6000)return json({error:'Sugestão muito grande.'},413);
    let body,proposal;try{body=JSON.parse(raw);proposal=validateSuggestion(body,data);}catch(e){return json({error:e.message||'Sugestão inválida.'},400);}
    const ip=request.headers.get('CF-Connecting-IP')||'unknown';const hash=new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode((env.ADMIN_KEY||'')+'|'+ip)));const clientHash=Array.from(hash,b=>b.toString(16).padStart(2,'0')).join('');
    const id=crypto.randomUUID(),now=new Date().toISOString(),since=new Date(Date.now()-3600000).toISOString();
    const inserted=await env.DB.prepare("INSERT INTO suggestions(id,payload,status,client_hash,created_at) SELECT ?,?,'pending',?,? WHERE (SELECT COUNT(*) FROM suggestions WHERE client_hash=? AND created_at>?)<10 AND COALESCE((SELECT enabled FROM suggestion_settings WHERE id=1),1)=1 RETURNING id").bind(id,JSON.stringify(proposal),clientHash,now,clientHash,since).first();
    if(inserted)return json({id,status:'pending',message:'Sugestão enviada para análise. Ela ainda não altera a parada.'},201);const latest=await env.DB.prepare('SELECT enabled FROM suggestion_settings WHERE id=1').first();return latest?.enabled===0?json({error:'O administrador pausou o recebimento de sugestões.'},403):json({error:'Limite de sugestões atingido. Tente novamente em uma hora.'},429);
  }
  if(!await authorize(request.headers.get('Authorization')?.replace(/^Bearer /,''),env.ADMIN_KEY))return json({error:'Chave de administrador inválida.'},401);
  if(path==='/api/suggestions'&&request.method==='GET'){const result=await env.DB.prepare("SELECT id,payload,status,created_at,reviewed_at FROM suggestions WHERE status='pending' ORDER BY created_at ASC LIMIT 100").all();return json({accepting,suggestions:result.results.map(r=>({id:r.id,...JSON.parse(r.payload),status:r.status,createdAt:r.created_at}))});}
  const match=path.match(/^\/api\/suggestions\/([a-f0-9-]{36})\/review$/);
  if(!match||request.method!=='POST')return json({error:'Operação não encontrada.'},404);
  const raw=await request.text();if(raw.length>300)return json({error:'Dados inválidos.'},400);let decision;try{decision=JSON.parse(raw).decision;}catch{return json({error:'Dados inválidos.'},400);}if(!['approve','reject'].includes(decision))return json({error:'Escolha aprovar ou rejeitar.'},400);
  const suggestion=await env.DB.prepare("SELECT payload,status FROM suggestions WHERE id=?").bind(match[1]).first();if(!suggestion)return json({error:'Sugestão não encontrada.'},404);if(suggestion.status!=='pending')return json({error:'Sugestão já analisada.'},409);
  const now=new Date().toISOString();if(decision==='reject'){const rejected=await env.DB.prepare("UPDATE suggestions SET status='rejected',reviewed_at=? WHERE id=? AND status='pending' RETURNING id").bind(now,match[1]).first();return rejected?json({status:'rejected'}):json({error:'Sugestão já analisada.'},409);}
  if(!row)return json({error:'Publique a tabela inicial antes de aprovar sugestões.'},409);
  let changed;try{changed=applySuggestion(data,JSON.parse(suggestion.payload),match[1]);validate(changed);}catch(e){return json({error:e.message||'Não foi possível aplicar a sugestão.'},400);}
  const operation=crypto.randomUUID();const batch=await env.DB.batch([
    env.DB.prepare("UPDATE suggestions SET status='approved',reviewed_at=?,review_token=? WHERE id=? AND status='pending' AND EXISTS(SELECT 1 FROM schedules WHERE id=1 AND revision=?)").bind(now,operation,match[1],row.revision),
    env.DB.prepare("UPDATE schedules SET payload=?,revision=revision+1,updated_at=? WHERE id=1 AND revision=? AND EXISTS(SELECT 1 FROM suggestions WHERE id=? AND status='approved' AND review_token=?)").bind(JSON.stringify(changed),now,row.revision,match[1],operation)
  ]);
  if(batch[0].meta.changes!==1||batch[1].meta.changes!==1)return json({error:'Dados alterados durante a análise. Atualize e tente novamente.'},409);
  return json({status:'approved',revision:row.revision+1});
}
