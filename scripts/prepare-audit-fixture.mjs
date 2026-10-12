import {cp,mkdir,readFile,writeFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const destination=path.resolve(process.argv[2]||'');
if(!process.argv[2]||destination===root||destination.startsWith(root+path.sep))throw Error('Use uma pasta de teste separada, fora do repositório.');
await cp(path.join(root,'shared'),path.join(destination,'shared'),{recursive:true});
for(const [project,pkg]of [['meu-onibus-android','br.com.meuonibus'],['meu-onibus-admin','br.com.meuonibus.admin']]){
const target=path.join(destination,project);await mkdir(target,{recursive:true});
await cp(path.join(root,project),target,{recursive:true,filter:p=>!p.split(/[\\/]/).some(name=>['build','build-manual','.gradle'].includes(name))&&!p.endsWith('.apk')});
const file=path.join(target,'app/src/main/java',pkg.replaceAll('.','/'),'CloudDatabase.java');
let source=await readFile(file,'utf8');source=source.replace('https://meu-onibus.miguel-angelo09.chatgpt.site/api/schedule','http://127.0.0.1:8187/api/schedule');await writeFile(file,source);
}
console.log('Fixtures isoladas: '+destination+'; API local somente. O mapa consulta a página pública do provedor, sem GPS real.');
