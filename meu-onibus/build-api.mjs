import {mkdir,readFile,writeFile,copyFile} from 'node:fs/promises';
await mkdir('dist/server',{recursive:true});
const body=await readFile('worker/body.js','utf8'),itineraries=await readFile('worker/itineraries.js','utf8');
const entry=await readFile('worker/index.js','utf8'),seed=await readFile('worker/seed.js','utf8'),map=await readFile('worker/map-page.js','utf8'),suggestions=await readFile('worker/suggestions.js','utf8');
const release=(await readFile('worker/app-release.js','utf8')).replace("import {releaseInfo,apkBase64} from './release-data.js';",(await readFile('worker/release-data.js','utf8')).replaceAll('export const','const'));
await writeFile('dist/server/index.js',entry.replace("import {validateItineraries} from './itineraries.js';",itineraries).replace("import {readBody} from './body.js';",body).replace("import seed from './seed.js';",seed.replace('export default','const seed =')).replace("import {mapPage} from './map-page.js';",map).replace("import {suggestionsApi} from './suggestions.js';",suggestions.replace("import {readBody} from './body.js';",'').replace("import {invalidateConnected} from './itineraries.js';",'')).replace("import {appReleaseApi} from './app-release.js';",release));
// Hosting configuration is supplied privately by the deployment environment.
