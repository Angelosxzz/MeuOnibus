import {releaseInfo,apkBase64} from './release-data.js';
let apkBytes;
const githubReleases='https://github.com/Angelosxzz/MeuOnibus-Downloads/releases/latest';
export function appReleaseApi(request){
  const path=new URL(request.url).pathname;
  if(path!=='/'&&path!=='/api/app-release'&&path!=='/downloads/Meu-Onibus.apk')return null;
  if(request.method!=='GET'&&request.method!=='HEAD')return new Response('Método não permitido',{status:405,headers:{Allow:'GET, HEAD','Cache-Control':'no-store'}});
  // Installed versions require a 200 APK response with this explicit Accept header.
  // Navigation and shared download links open the latest GitHub Release instead.
  if(path==='/'||(path==='/downloads/Meu-Onibus.apk'&&!request.headers.get('Accept')?.split(',').some(type=>type.trim().split(';')[0]==='application/vnd.android.package-archive')))return new Response(null,{status:302,headers:{Location:githubReleases,'Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
  if(path==='/api/app-release')return new Response(request.method==='HEAD'?null:JSON.stringify(releaseInfo),{headers:{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
  if(!apkBytes)apkBytes=Uint8Array.from(atob(apkBase64),c=>c.charCodeAt(0));
  return new Response(request.method==='HEAD'?null:apkBytes,{headers:{'Content-Type':'application/vnd.android.package-archive','Content-Disposition':`attachment; filename="Meu-Onibus-${releaseInfo.versionName}.apk"`,'Content-Length':String(apkBytes.length),'Cache-Control':'no-store',ETag:`"${releaseInfo.sha256}"`,'X-Content-Type-Options':'nosniff'}});
}
