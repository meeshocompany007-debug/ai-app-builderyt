import test from 'node:test';
import assert from 'node:assert/strict';
import {Readable} from 'node:stream';
import {mkdtemp,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {starter,render} from '../core.mjs';
import {androidFiles} from '../android.mjs';
const dir=await mkdtemp(join(tmpdir(),'builder-test-'));
process.env.DATA_DIR=dir;process.env.STUDIO_TEST='1';delete process.env.GEMINI_API_KEY;delete process.env.ENABLE_APK;
const {handler}=await import('../server.mjs');
async function req(path,body,cookie=''){
 const r=Readable.from(body===undefined?[]:[Buffer.from(JSON.stringify(body))]);Object.assign(r,{url:path,method:body===undefined?'GET':'POST',headers:{host:'local','content-type':'application/json',cookie},socket:{remoteAddress:'test'}});
 const result={statusCode:200,headers:{},setHeader(k,v){this.headers[k]=v},end(x){this.body=x}};
 await handler(r,result);return result;
}
test('owner-only versions, exports and run limits',async()=>{
 const register=async name=>(await req('/api/register',{name,password:'long-pass-123456'})).headers['Set-Cookie'].split(';')[0];
 const a=await register('alice'),b=await register('bob');
 const create={kind:'Android App',idea:'खर्च <script>test</script>'};
 const first=await req('/api/projects',create,a);assert.equal(first.statusCode,200);
 const {id}=JSON.parse(first.body),base='/api/projects/'+id;
 assert.equal((await req(base,undefined,b)).statusCode,404);
 assert.equal((await req(base+'/zip',undefined,b)).statusCode,404);
 assert.equal((await req(base+'/html')).statusCode,401);
 const edited=await req('/api/projects',{...create,id,version:1,idea:'Task list'},a);assert.equal(JSON.parse(edited.body).v,2);
 assert.equal((await req('/api/projects',{...create,id,version:1},a)).statusCode,409);
 const html=(await req(base+'/html?v=1',undefined,a)).body;assert.ok(html.includes('&lt;script&gt;test'));assert.ok(html.includes('कुल ₹'));
 const zip=await req(base+'/zip',undefined,a);assert.equal(zip.body.readUInt16LE(0),0x4b50);
 assert.equal((await req(base+'/apk',{},a)).statusCode,503);assert.equal((await req('/api/projects',{...create,ai:true},a)).statusCode,503);
 assert.equal((await req('/api/logout',{},a)).statusCode,200);assert.equal((await req(base,undefined,a)).statusCode,401);
});
test('Android output shares website and keeps package stable on update',()=>{
 const spec=starter('notes');const f=androidFiles(spec,'abc',1),g=androidFiles(spec,'abc',2);
 assert.equal(f['app/src/main/assets/index.html'],render(spec,'abc'));assert.match(g['app/build.gradle'],/versionCode 2/);
 assert.equal(f['app/build.gradle'].match(/applicationId '([^']+)/)[1],g['app/build.gradle'].match(/applicationId '([^']+)/)[1]);
});
test.after(()=>rm(dir,{recursive:true,force:true}));
