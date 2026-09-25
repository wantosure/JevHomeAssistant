import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import worker,{buildGroups,groupCandidates} from '../worker/index.js';
import {createTestD1} from './test-d1.mjs';
const DB=createTestD1();
const data=JSON.parse(readFileSync(new URL('../mijia-100-devices.json',import.meta.url)));
const live=process.argv.includes('--live');
let key='mock';
if(live){let raw=readFileSync(new URL('../.key',import.meta.url),'utf8').replace(/^\uFEFF/,'').trim();if(raw.startsWith('{')){const v=JSON.parse(raw);raw=v.TYPESAFE_API_KEY||v.api_key||v.apikey||v.key;}else if(raw.toUpperCase().startsWith('TYPESAFE_API_KEY='))raw=raw.slice(raw.indexOf('=')+1).trim().replace(/^["']|["']$/g,'');key=raw;}
const cases=[
 {text:'关所有灯',room:'主卧',scope:'all',type:['灯具','灯带'],ops:{power:'0'}},
 {text:'开所有窗帘',room:'主卧',scope:'all',type:['窗帘'],ops:{motion:'open'}},
 {text:'卧室所有灯亮度四十',room:'客厅',scope:'master',type:['灯具','灯带'],ops:{brightness:'n0'}},
 {text:'所有空调制冷温度二十五',room:'主卧',scope:'all',type:['空调'],ops:{mode:'cool',target_temperature:'n0'}},
 {text:'关闭所有卧室的灯',room:'客厅',scope:'bedrooms',type:['灯具','灯带'],ops:{power:'0'}},
 {text:'当前房间所有灯亮度四十',room:'客厅',scope:data.rooms.find(r=>r.name==='客厅').id,type:['灯具','灯带'],ops:{brightness:'n0'}},
];
let active, confidence=1;
if(!live)globalThis.fetch=async(url,options)=>{
 const {questions}=JSON.parse(options.body);
 if(questions.intent)return Response.json({answers:{intent:{choice:'home_control',confidence:1}}});
 const target=buildGroups(data).find(g=>g.scope===active.scope&&(active.type.length===2?g.id.endsWith(':lights'):g.device_ids.every(id=>active.type.includes(data.devices.find(d=>d.id===id).type))));
 assert.ok(questions.group.criteria[target.id]);
 const answers={group:{choice:target.id,confidence}};
 for(const q of Object.keys(questions).filter(q=>q.startsWith('set_')))answers[q]={choice:active.ops[q.slice(4)]||'no_change',confidence:1};
 return Response.json({answers,model:'mock',usage:{input_tokens:0}});
};
async function run(c,i){active=c;const response=await worker.fetch(new Request('https://example.test/api/process',{method:'POST',headers:{'content-type':'application/json','cf-connecting-ip':'test-'+i,'x-client-id':'a0000000-0000-4000-8000-000000000001'},body:JSON.stringify({text:c.text,room:c.room,device_type:'灯具',threshold:0.35})}),{JEV_API_KEY:key,DB});return (await response.text()).trim().split('\n').map(JSON.parse);}
for(const [i,c] of cases.entries()){
 const events=await run(c,i), batch=events.find(e=>e.kind==='execute_batch');
 assert.ok(batch,JSON.stringify(events.filter(e=>['error','result','decision'].includes(e.kind))));
 const rooms=c.scope==='all'?null:c.scope==='bedrooms'?['主卧','次卧','儿童房']:[data.rooms.find(r=>r.id===c.scope).name];
 const expected=data.devices.filter(d=>c.type.includes(d.type)&&(!rooms||rooms.includes(d.room))).map(d=>d.id);
 assert.deepEqual(batch.results.map(r=>r.device_id),expected);assert.equal(batch.skipped.length,0);
 for(const r of batch.results)for(const [k,v] of Object.entries(c.ops))assert.equal(String(r.after[k]),v==='n0'?(k==='target_temperature'?'25':'40'):v);
 if(c.type[0]==='窗帘')assert.ok(batch.results.every(r=>r.after.position===100));
 console.log(JSON.stringify({mode:live?'real JEV':'mock',command:c.text,count:batch.results.length,group:batch.group_name}));
}
assert.equal(groupCandidates(data,'当前房间所有灯',null).length,0);
if(!live){
 confidence=0.1;assert.ok(!(await run(cases[0],20)).some(e=>e.kind==='execute_batch'));confidence=1;
 assert.ok(!(await run({...cases[0],text:'除了主卧，关闭所有灯'},21)).some(e=>e.kind==='execute_batch'));
 const bad=await run({...cases[2],text:'卧室所有灯亮度一百二十'},22);
 const batch=bad.find(e=>e.kind==='execute_batch');assert.equal(batch.results.length,0);assert.equal(batch.skipped.length,5);
}
