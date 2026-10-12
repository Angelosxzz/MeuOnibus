import {releaseInfo,apkBase64} from './release-data.js';
let apkBytes;
export function appReleaseApi(request){
  const path=new URL(request.url).pathname;
  if(path!=='/api/app-release'&&path!=='/downloads/Meu-Onibus.apk')return null;
  if(request.method!=='GET'&&request.method!=='HEAD')return new Response('Método não permitido',{status:405,headers:{Allow:'GET, HEAD','Cache-Control':'no-store'}});
  if(path==='/api/app-release')return new Response(request.method==='HEAD'?null:JSON.stringify(releaseInfo),{headers:{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
  if(!apkBytes)apkBytes=Uint8Array.from(atob(apkBase64),c=>c.charCodeAt(0));
  return new Response(request.method==='HEAD'?null:apkBytes,{headers:{'Content-Type':'application/vnd.android.package-archive','Content-Disposition':`attachment; filename="Meu-Onibus-${releaseInfo.versionName}.apk"`,'Content-Length':String(apkBytes.length),'Cache-Control':'no-store',ETag:`"${releaseInfo.sha256}"`,'X-Content-Type-Options':'nosniff'}});
}
