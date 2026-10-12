import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {readFile} from 'node:fs/promises';
import worker from './worker/index.js';
import seed from './worker/seed.js';

async function fixture(){
  const db=new DatabaseSync(':memory:');
  for(const file of ['0000_schedule.sql','0001_map_config.sql','0002_suggestions.sql'])db.exec(await readFile(new URL('./drizzle/'+file,import.meta.url),'utf8'));
  db.prepare('INSERT INTO schedules VALUES(1,?,2,?)').run(JSON.stringify(seed),'2026-10-11T19:00:00Z');
  const DB={prepare(sql){let args=[];return {bind(...values){args=values;return this;},async first(){return db.prepare(sql).get(...args)||null;},async all(){return {results:db.prepare(sql).all(...args)};},async run(){const result=db.prepare(sql).run(...args);return {meta:{changes:Number(result.changes)}};}};},async batch(statements){db.exec('BEGIN');try{const results=[];for(const statement of statements)results.push(await statement.run());db.exec('COMMIT');return results;}catch(error){db.exec('ROLLBACK');throw error;}}};
  return {db,env:{DB,ADMIN_KEY:'test-only'}};
}
const request=(path,options={})=>new Request('https://example.test'+path,options);
const tests=[
  ['Falha assíncrona retorna JSON sem detalhes internos',async()=>{
    const env={DB:{prepare(){throw new Error('PRIVATE_DATABASE_DETAILS');}}};
    const response=await worker.fetch(request('/api/suggestions/settings'),env);
    assert.equal(response.status,500);assert.match(response.headers.get('Content-Type'),/application\/json/);
    assert.ok(!(await response.text()).includes('PRIVATE_DATABASE_DETAILS'));
  }],
  ['Banco ausente continua retornando 503',async()=>{
    const response=await worker.fetch(request('/api/suggestions/settings'),{});assert.equal(response.status,503);
  }],
  ['Revalidação não transfere a tabela inalterada',async()=>{
    const {env,db}=await fixture();try{
      const first=await worker.fetch(request('/api/schedule'),env);assert.equal(first.headers.get('ETag'),'"schedule-2"');assert.equal(first.status,200);
      for(const value of ['"schedule-2"','W/"schedule-2"','"schedule-1", W/"schedule-2"']){
        const same=await worker.fetch(request('/api/schedule',{headers:{'If-None-Match':value}}),env);assert.equal(same.status,304);assert.equal((await same.arrayBuffer()).byteLength,0);assert.equal(same.headers.get('ETag'),'"schedule-2"');
      }
      const old=await worker.fetch(request('/api/schedule',{headers:{'If-None-Match':'"schedule-1"'}}),env);assert.equal(old.status,200);assert.equal((await old.json()).revision,2);
      const next=await worker.fetch(request('/api/schedule',{method:'PUT',headers:{Authorization:'Bearer test-only'},body:JSON.stringify({data:seed,revision:2})}),env);assert.equal(next.status,200);
      const changed=await worker.fetch(request('/api/schedule',{headers:{'If-None-Match':'"schedule-2"'}}),env);assert.equal(changed.status,200);assert.equal(changed.headers.get('ETag'),'"schedule-3"');
    }finally{db.close();}
  }],
  ['Limite de sugestão é em bytes UTF-8',async()=>{
    const {env,db}=await fixture();try{
      const body=JSON.stringify({operation:'update',target:'stop:Rodoviária',name:'Nome de teste',lat:null,lon:null,padding:'é'.repeat(4000)});
      assert.ok(body.length<6000);assert.ok(new TextEncoder().encode(body).length>6000);
      const response=await worker.fetch(request('/api/suggestions',{method:'POST',body}),env);assert.equal(response.status,413);assert.equal(db.prepare('SELECT COUNT(*) AS count FROM suggestions').get().count,0);
    }finally{db.close();}
  }],
  ['Pedido grande é interrompido antes de ler o restante',async()=>{
    const {env,db}=await fixture();try{
      let sent=false;
      const body=new ReadableStream({pull(controller){if(!sent){sent=true;controller.enqueue(new Uint8Array(6500));}else controller.error(new Error('Restante não deve ser consumido'));}});
      const response=await worker.fetch(request('/api/suggestions',{method:'POST',body,duplex:'half'}),env);assert.equal(response.status,413);
    }finally{db.close();}
  }],
  ['Mapa bloqueia frames e fixa integridade da biblioteca',async()=>{
    const response=await worker.fetch(request('/map-picker'),{});const policy=response.headers.get('Content-Security-Policy');assert.ok(policy&&policy.includes("frame-src 'none'"));assert.ok(policy.includes("object-src 'none'"));
    const html=await response.text();assert.ok(html.includes('integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo="'));assert.ok(html.includes('crossorigin="anonymous"'));
    const nonce=policy.match(/'nonce-([^']+)'/)[1];assert.ok(html.includes('nonce="'+nonce+'"'));
  }]
];
let failures=0;
for(const [name,test]of tests){try{await test();console.log('PASS: '+name);}catch(error){failures++;console.error('FAIL: '+name+' — '+error.message);}}
if(failures)process.exitCode=1;
