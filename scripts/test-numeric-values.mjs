import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {spawnSync} from 'node:child_process';
import worker, {numericCandidates} from '../worker/index.js';
import {createTestD1} from './test-d1.mjs';

const cases = [
  ['主卧灯亮度四十，色温三千', ['40','3000']],
  ['色温三千，亮度四十', ['3000','40']],
  ['亮度百分之四十，色温3000', ['40','3000']],
  ['亮度40，色温三千二百', ['40','3200']],
  ['温度二十五点五', ['25.5']],
  ['冷冻温度负十八', ['-18']],
  ['亮度零', ['0']],
  ['色温两千七百', ['2700']],
  ['亮度四十和40', ['40']],
  ['开主卧灯', []],
];
for (const [text, expected] of cases) assert.deepEqual(numericCandidates(text), expected);
const py=spawnSync('python',['-X','utf8','-c','import json,sys; from numeric_values import numeric_candidates; print(json.dumps([numeric_candidates(t) for t in json.load(sys.stdin)]))'],{input:JSON.stringify(cases.map(c=>c[0])),encoding:'utf8'});
assert.equal(py.status,0,py.stderr);
assert.deepEqual(JSON.parse(py.stdout),cases.map(c=>c[1]));

const live=process.argv.includes('--live');
let key='mock';
if(live){
  let raw=readFileSync(new URL('../.key',import.meta.url),'utf8').replace(/^\uFEFF/,'').trim();
  if(raw.startsWith('{')) {const obj=JSON.parse(raw);raw=obj.TYPESAFE_API_KEY||obj.api_key||obj.apikey||obj.key;}
  else if(raw.toUpperCase().startsWith('TYPESAFE_API_KEY=')) raw=raw.slice(raw.indexOf('=')+1).trim();
  key=raw;
} else {
  globalThis.fetch=async(url,options)=>{
    const {state,questions}=JSON.parse(options.body);
    const answers={};
    if(questions.intent) answers.intent={choice:'home_control',confidence:1};
    else {
      assert.deepEqual(state.numeric_candidates,['40','3000']);
      for(const name of ['set_brightness','set_color_temperature']){
        assert.ok(questions[name].criteria.n0);
        assert.ok(questions[name].criteria.n1);
      }
      Object.assign(answers,{device:{choice:'MJ-001',confidence:1},set_brightness:{choice:'n0',confidence:1},set_color_temperature:{choice:'n1',confidence:1}});
    }
    return Response.json({answers,model:'mock',usage:{input_tokens:0}});
  };
}
const response=await worker.fetch(new Request('https://example.test/api/process',{method:'POST',headers:{'Content-Type':'application/json','X-Client-Id':'a0000000-0000-4000-8000-000000000001'},body:JSON.stringify({text:'主卧灯亮度四十，色温三千',room:'主卧',threshold:0.35})}),{JEV_API_KEY:key,DB:createTestD1()});
const events=(await response.text()).trim().split('\n').map(JSON.parse);
const execute=events.find(e=>e.kind==='execute');
assert.ok(execute,JSON.stringify(events.filter(e=>['error','result','decision'].includes(e.kind))));
assert.equal(execute.device_id,'MJ-001');
assert.equal(execute.after.brightness,40);
assert.equal(execute.after.color_temperature,3000);
assert.equal(execute.operations.length,2);
console.log(JSON.stringify({mode:live?'real JEV':'mock',numericCases:cases.length,device:execute.device_id,operations:execute.operations.map(({key,value})=>({key,value}))}));
