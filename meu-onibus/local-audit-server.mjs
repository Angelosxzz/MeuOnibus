import {createServer} from 'node:http';
import {Readable} from 'node:stream';
import {DatabaseSync} from 'node:sqlite';
import {readFile} from 'node:fs/promises';
import worker from './worker/index.js';
import seed from './worker/seed.js';
const db=new DatabaseSync(':memory:');
for(const file of ['0000_schedule.sql','0001_map_config.sql','0002_suggestions.sql'])db.exec(await readFile(new URL('./drizzle/'+file,import.meta.url),'utf8'));
db.prepare('INSERT INTO schedules VALUES(1,?,2,?)').run(JSON.stringify(seed),'2026-10-11T19:00:00Z');
const DB={prepare(sql){let args=[];return {bind(...values){args=values;return this;},async first(){return db.prepare(sql).get(...args)||null;},async all(){return {results:db.prepare(sql).all(...args)};},async run(){const result=db.prepare(sql).run(...args);return {meta:{changes:Number(result.changes)}};}};},async batch(statements){db.exec('BEGIN');try{const results=[];for(const statement of statements)results.push(await statement.run());db.exec('COMMIT');return results;}catch(error){db.exec('ROLLBACK');throw error;}}};
const port=Number(process.env.AUDIT_PORT||8187);
const server=createServer(async(req,res)=>{
  try{
    const options={method:req.method,headers:req.headers};
    if(!['GET','HEAD'].includes(req.method)){options.body=Readable.toWeb(req);options.duplex='half';}
    const response=await worker.fetch(new Request('http://127.0.0.1:'+port+req.url,options),{DB,ADMIN_KEY:'test-only'});
    res.writeHead(response.status,Object.fromEntries(response.headers));res.end(Buffer.from(await response.arrayBuffer()));
  }catch{res.writeHead(500,{'Content-Type':'application/json'});res.end('{"error":"Falha do servidor local de teste"}');}
});
server.listen(port,'127.0.0.1',()=>console.log('QA API: http://127.0.0.1:'+port+' — SQLite em memória, sem conexão ao D1 de produção. Chave fictícia: test-only.'));
process.on('SIGINT',()=>server.close(()=>{db.close();process.exit(0);}));
