const point=p=>Array.isArray(p)&&p.length===2&&p.every(Number.isFinite)&&Math.abs(p[0])<=90&&Math.abs(p[1])<=180;
function metres(a,b){const radians=value=>value*Math.PI/180;const lat=radians(b[0]-a[0]),lon=radians(b[1]-a[1]);const h=Math.sin(lat/2)**2+Math.cos(radians(a[0]))*Math.cos(radians(b[0]))*Math.sin(lon/2)**2;return 6371000*2*Math.atan2(Math.sqrt(Math.min(1,h)),Math.sqrt(Math.max(0,1-h)));}
export function validateItineraries(data,route){
  if(route.roadSegments===undefined)return;
  if(!Array.isArray(route.roadSegments)||route.roadSegments.length>20)throw Error('Máximo de 20 trechos de itinerário por linha.');
  const keys=new Set();
  for(const s of route.roadSegments){
    if(s?.itineraryConfirmed!==true)continue; // Legacy driving geometries remain readable, never silently confirmed.
    const a=s.from===0?route.origin:data.stops[s.from],b=s.to===0?route.origin:data.stops[s.to];
    const error=()=>{throw Error('Itinerário inválido: confira origem, sentido, paradas e pontos a no máximo 250 m entre si.');};
    if(!Number.isInteger(s.from)||s.from<0||s.from>3||!Number.isInteger(s.to)||s.to<0||s.to>3||s.from===s.to||s.geometryVersion!==3||!['admin-traced','official'].includes(s.geometrySource)||typeof s.geometryReference!=='string'||!s.geometryReference.trim()||s.geometryReference.length>200)error();
    if(!a?.confirmed||!b?.confirmed||!point([a.lat,a.lon])||!point([b.lat,b.lon])||!Array.isArray(s.path)||s.path.length<2||s.path.length>1000||!s.path.every(point))error();
    if(!point([s.fromLat,s.fromLon])||!point([s.toLat,s.toLon])||metres([a.lat,a.lon],[s.fromLat,s.fromLon])>=5||metres([b.lat,b.lon],[s.toLat,s.toLon])>=5)error();
    if(!Number.isFinite(s.travelMinutes)||s.travelMinutes<=0||s.travelMinutes>180||typeof s.destination!=='string'||!s.destination.trim()||s.destination.length>100||typeof s.label!=='string'||!s.label.trim()||s.label.length>100)error();
    if(metres(s.path[0],[a.lat,a.lon])>50||metres(s.path.at(-1),[b.lat,b.lon])>50)error();
    let length=0;for(let i=1;i<s.path.length;i++){const distance=metres(s.path[i-1],s.path[i]);if(distance>250)error();length+=distance;}if(length<1)error();
    const key=s.from+':'+s.to;if(keys.has(key))throw Error('Trecho confirmado duplicado.');keys.add(key);
  }
}
export function invalidateConnected(data,target){
  const origin=target.startsWith('origin:')?target.slice(7):null;
  const stop=target.startsWith('stop:')?data.stops.findIndex(s=>'stop:'+s.name===target):-1;
  if(origin===null&&stop<1)return;
  for(const route of data.routes){if(origin!==null&&route.name!==origin)continue;const index=origin===null?stop:0;for(const segment of route.roadSegments||[])if(segment.from===index||segment.to===index)segment.itineraryConfirmed=false;}
}
