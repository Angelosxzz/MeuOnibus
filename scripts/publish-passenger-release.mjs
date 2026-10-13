import {execFileSync} from 'node:child_process';
import {readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const repo='Angelosxzz/MeuOnibus-Downloads';
if(process.argv[2]!=='--publish')throw Error('Use --publish para publicar somente o APK dos passageiros.');
const config=JSON.parse(await readFile(path.join(root,'meu-onibus/release-config.json'),'utf8'));
const apk=await readFile(path.join(root,'meu-onibus-android/Meu-Onibus.apk'));
const manifest=await readFile(path.join(root,'meu-onibus-android/app/src/main/AndroidManifest.xml'),'utf8');
if(!/^\d+\.\d+\.\d+$/.test(config.versionName)||Number(manifest.match(/versionCode="(\d+)"/)[1])!==config.versionCode||manifest.match(/versionName="([^"]+)"/)[1]!==config.versionName||apk.subarray(0,4).toString('hex')!=='504b0304')throw Error('Versão ou APK inconsistentes.');
const sha=createHash('sha256').update(apk).digest('hex');
const raw=execFileSync('C:/Program Files/Git/cmd/git.exe',['credential','fill'],{input:'protocol=https\nhost=github.com\n\n',encoding:'utf8',env:{...process.env,GIT_TERMINAL_PROMPT:'0',GCM_INTERACTIVE:'never'},stdio:['pipe','pipe','pipe']});
const credential=Object.fromEntries(raw.trim().split(/\r?\n/).map(line=>{const i=line.indexOf('=');return [line.slice(0,i),line.slice(i+1).trim()];}));
if(!credential.password)throw Error('Autenticação GitHub indisponível.');
const headers={Authorization:'Bearer '+credential.password,Accept:'application/vnd.github+json','User-Agent':'Meu-Onibus-release','X-GitHub-Api-Version':'2022-11-28'};
async function api(route,options={}){const r=await fetch('https://api.github.com'+route,{...options,headers:{...headers,...options.headers}});if(!r.ok)throw Error('GitHub HTTP '+r.status);return r.status===204?null:r.json();}
const base='/repos/'+repo;
const check=await fetch('https://api.github.com'+base,{headers});
let state;if(check.status===404)state=await api('/user/repos',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:'MeuOnibus-Downloads',description:'Downloads e atualizações do aplicativo Android Meu Ônibus para passageiros.',private:false,auto_init:true})});else if(check.ok)state=await check.json();else throw Error('Consulta GitHub HTTP '+check.status);
if(state.full_name.toLowerCase()!==repo.toLowerCase()||state.private)throw Error('Destino de publicação incorreto.');
const readme='# Meu Ônibus — Downloads\n\nAplicativo Android para passageiros de Tabatinga, Umarizeiras e Amanari, em Maranguape.\n\n## Baixar e atualizar\n\nAbra [a versão mais recente](https://github.com/'+repo+'/releases/latest) e baixe o arquivo APK na seção **Assets**. Instale sobre a versão anterior, sem desinstalar. Android 8 ou superior.\n\nTodas as versões publicadas ficam na [aba Releases](https://github.com/'+repo+'/releases).\n\nAs previsões usam horários e itinerários cadastrados; não há rastreamento ao vivo dos ônibus.\n\nEste repositório contém somente informações de download e APKs dos passageiros. O código do projeto e o app administrador são mantidos separadamente.\n';
const previous=await api(base+'/contents/README.md');
if(Buffer.from(previous.content,'base64').toString('utf8')!==readme)await api(base+'/contents/README.md',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:'docs: instalação e atualizações dos passageiros',sha:previous.sha,content:Buffer.from(readme).toString('base64')})});
const tag='v'+config.versionName;
const releaseCheck=await fetch('https://api.github.com'+base+'/releases/tags/'+tag,{headers});
let release;if(releaseCheck.status===404)release=await api(base+'/releases',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({tag_name:tag,name:'Meu Ônibus '+config.versionName,target_commitish:state.default_branch,draft:false,prerelease:false,make_latest:'true',body:config.notes+'\n\nBaixe o APK em Assets e instale sobre a versão anterior.\n\nSHA-256: `'+sha+'`\n\nAndroid 8 ou superior. APK assinado com o mesmo certificado da versão anterior.'})});else if(releaseCheck.ok)release=await releaseCheck.json();else throw Error('Consulta release HTTP '+releaseCheck.status);
const filename='Meu-Onibus-'+config.versionName+'.apk';
const existing=release.assets.find(a=>a.name===filename);
if(existing){const r=await fetch(existing.browser_download_url);if(!r.ok||createHash('sha256').update(Buffer.from(await r.arrayBuffer())).digest('hex')!==sha)throw Error('Release existente tem conteúdo diferente; não sobrescrever.');}
else {const url=release.upload_url.replace(/\{.*\}$/,'')+'?name='+encodeURIComponent(filename);const r=await fetch(url,{method:'POST',headers:{...headers,'Content-Type':'application/vnd.android.package-archive'},body:apk});if(!r.ok)throw Error('Upload GitHub HTTP '+r.status);const asset=await r.json();if(asset.size!==apk.length)throw Error('Tamanho publicado diferente.');}
const verified=await api(base+'/releases/'+release.id);if(verified.assets.some(a=>/admin/i.test(a.name)))throw Error('Destino público contém anexo administrativo inesperado.');
console.log(JSON.stringify({repository:repo,release:verified.html_url,latest:'https://github.com/'+repo+'/releases/latest',sha256:sha,assets:verified.assets.map(a=>a.name)}));
