import assert from 'node:assert/strict';
import {validate} from './worker/index.js';
import {applySuggestion} from './worker/suggestions.js';
import seed from './worker/seed.js';

function data(){
  const value=structuredClone(seed);value.stops[1]={name:'Rodoviária',lat:0,lon:.002,confirmed:true};value.routes[0].origin={lat:0,lon:0,confirmed:true};
  value.routes[0].roadSegments=[{from:0,to:1,fromLat:0,fromLon:0,toLat:0,toLon:.002,destination:'Rodoviária',label:'Tabatinga → Rodoviária',travelMinutes:21,paired:true,geometryVersion:3,itineraryConfirmed:true,geometrySource:'admin-traced',geometryReference:'Conferido em viagem',path:[[0,0],[0,.001],[0,.002]]}];return value;
}
let failures=0;
function check(name,test){try{test();console.log('PASS: '+name);}catch(error){failures++;console.error('FAIL: '+name+' — '+error.message);}}
check('Percurso confirmado aceita só geometria íntegra',()=>{
  assert.doesNotThrow(()=>validate(data()));
  const bad=data();bad.routes[0].roadSegments[0].path=[[0,0],[91,0]];assert.throws(()=>validate(bad),/itinerário|trajeto/i);
});
check('Um caminho de carro não pode ser confirmado como itinerário',()=>{const bad=data();bad.routes[0].roadSegments[0].geometrySource='driving';assert.throws(()=>validate(bad),/itinerário|trajeto/i);});
check('Percurso invertido e atalhos longos são recusados',()=>{for(const path of [[[0,.002],[0,.001],[0,0]],[[0,0],[0,.003]]]){const bad=data();bad.routes[0].roadSegments[0].path=path;assert.throws(()=>validate(bad),/itinerário|trajeto/i);}});
check('Proposta aprovada que move a parada invalida só os trechos ligados',()=>{
  const value=data();value.routes[0].roadSegments.push({from:2,to:3,itineraryConfirmed:true});
  const changed=applySuggestion(value,{operation:'update',target:'origin:Tabatinga',name:'',lat:0,lon:.01},'test');
  assert.equal(changed.routes[0].roadSegments[0].itineraryConfirmed,false);assert.equal(changed.routes[0].roadSegments[1].itineraryConfirmed,true);
  assert.equal(value.routes[0].roadSegments[0].itineraryConfirmed,true,'Input remains intact');
});
check('Alteração só de nome não invalida o percurso',()=>{const value=data();const changed=applySuggestion(value,{operation:'update',target:'origin:Tabatinga',name:'Novo nome',lat:null,lon:null},'test');assert.equal(changed.routes[0].roadSegments[0].itineraryConfirmed,true);});
check('Geometria antiga continua legível mas não é confirmada automaticamente',()=>{const value=data();delete value.routes[0].roadSegments[0].itineraryConfirmed;value.routes[0].roadSegments[0].geometryVersion=2;assert.doesNotThrow(()=>validate(value));assert.notEqual(value.routes[0].roadSegments[0].itineraryConfirmed,true);});
if(failures)process.exitCode=1;
