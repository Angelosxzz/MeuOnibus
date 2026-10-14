import {cp,copyFile,mkdir} from 'node:fs/promises';
import {execFileSync} from 'node:child_process';
execFileSync(process.execPath,['build-api.mjs'],{stdio:'inherit'});
await mkdir('dist/cloudflare/web',{recursive:true});
await cp('dist/web','dist/cloudflare/web',{recursive:true});
await copyFile('cloudflare-web-worker.js','dist/cloudflare/_worker.js');
console.log('Cloudflare Pages: dist/cloudflare (passenger web assets and API gateway only)');
