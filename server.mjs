import http from 'node:http';
import {api,init} from './services/api.mjs';
import {proxy} from './services/publisher/proxy.mjs';
import {DatabaseSync} from 'node:sqlite';
import {randomBytes,randomUUID,createHash,scrypt,timingSafeEqual} from 'node:crypto';
import {promisify} from 'node:util';
import {mkdir,mkdtemp,readFile,rm} from 'node:fs/promises';
import {join} from 'node:path';
import {tmpdir} from 'node:os';
import {execFile} from 'node:child_process';
import {starter,validate,render} from './core.mjs';
import {androidFiles,materialize} from './android.mjs';
const run=promisify(execFile),hash=promisify(scrypt),digest=x=>createHash('sha256').update(x).digest('hex');
const data=process.env.DATA_DIR||'data';await mkdir(data,{recursive:true});
const db=new DatabaseSync(join(data,'studio.sqlite'));db.exec(`PRAGMA journal_mode=WAL;
CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY, name TEXT UNIQUE, password TEXT, salt TEXT);
CREATE TABLE IF NOT EXISTS sessions(token TEXT PRIMARY KEY,user TEXT,expires INTEGER);
CREATE TABLE IF NOT EXISTS projects(id TEXT PRIMARY KEY,user TEXT,kind TEXT);
CREATE TABLE IF NOT EXISTS versions(id TEXT,v INTEGER,idea TEXT,spec TEXT,PRIMARY KEY(id,v));
CREATE TABLE IF NOT EXISTS usage(day TEXT,bucket TEXT,n INTEGER,PRIMARY KEY(day,bucket));`);
init(db);
const q=s=>db.prepare(s);
function fail(status,message){throw Object.assign(new Error(message),{status})}
function limit(bucket,max,day=new Date().toISOString().slice(0,10)){
 const r=q('INSERT INTO usage VALUES(?,?,1) ON CONFLICT(day,bucket) DO UPDATE SET n=n+1 WHERE n<?').run(day,bucket,max);
 if(!r.changes)fail(429,'इस समय की सीमा पूरी हुई। बाद में कोशिश करें।');
}
let building=false;
const reserved=new Set();
const cap=Math.floor(Number(process.env.AI_DAILY_LIMIT)||0);
const model=process.env.GEMINI_MODEL;
const aiEnabled=Boolean(cap>0 && model && /^[a-zA-Z0-9._-]+$/.test(model) && process.env.GEMINI_API_KEY);
async function generate(idea,old){
 if(!aiEnabled)fail(503,'AI generation अभी configure नहीं है।');
 limit('ai',cap);
 const response=await fetch('https://generativelanguage.googleapis.com/v1beta/models/'+model+':generateContent',{method:'POST',signal:AbortSignal.timeout(45000),headers:{'Content-Type':'application/json','x-goog-api-key':process.env.GEMINI_API_KEY},body:JSON.stringify({systemInstruction:{parts:[{text:'Design a website or mobile app layout for the request. Return JSON only: title (string), intro (string), sections (array of heading/body string objects), feature (one of notes,tasks,expenses,none). Use the language requested. For an edit preserve unaffected content.'}]},contents:[{parts:[{text:JSON.stringify({request:idea,previous:old})}]}],generationConfig:{responseMimeType:'application/json',maxOutputTokens:3500}})});
 if(!response.ok)fail(502,'AI provider ने request पूरी नहीं की।');
 try{return validate(JSON.parse((await response.json()).candidates[0].content.parts.map(p=>p.text||'').join('')))}catch{fail(502,'AI से मान्य layout नहीं मिला।')}
}
function json(res,x){res.setHeader('Content-Type','application/json; charset=utf-8');res.end(JSON.stringify(x))}
async function body(req){let chunks=[],n=0;for await(const b of req){n+=b.length;if(n>24000)fail(413,'Request बहुत बड़ी है।');chunks.push(b)}try{return JSON.parse(Buffer.concat(chunks))}catch{fail(400,'Invalid JSON')}}
function session(req){const t=(req.headers.cookie||'').match(/(?:^|; *)studio=([a-f0-9]{64})(?:;|$)/)?.[1];return t&&q('SELECT user FROM sessions WHERE token=? AND expires>?').get(digest(t),Date.now())?.user}
function load(id,uid,v){const row=q('SELECT p.kind,x.* FROM projects p JOIN versions x ON p.id=x.id WHERE p.user=? AND p.id=? AND (? IS NULL OR x.v=?) ORDER BY x.v DESC LIMIT 1').get(uid,id,v,v);if(!row)fail(404,'Project नहीं मिला।');row.spec=JSON.parse(row.spec);return row}
const files={'/':['apps/studio-web/index.html','text/html'],'/studio':['apps/studio-web/index.html','text/html'],'/builder.js':['apps/studio-web/app.js','text/javascript'],'/builder.css':['apps/studio-web/style.css','text/css'],'/live':['build/site/index.html','text/html'],'/studio.js':['public/studio.js','text/javascript']};
export async function handler(req,res){try{
 if(proxy(req,res,db))return;
 const url=new URL(req.url,'http://localhost'),p=url.pathname,m=req.method;
 res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Cache-Control','no-store');
 if(m==='GET' && Object.hasOwn(files,p)){const [f,t]=files[p];res.setHeader('Content-Type',t+'; charset=utf-8');res.end(await readFile(new URL(f,import.meta.url)));return}
 if(m==='POST' && (!req.headers['content-type']?.startsWith('application/json') || (req.headers.origin && new URL(req.headers.origin).host!==req.headers.host)))fail(403,'Invalid request origin');
 if(m==='GET' && p==='/api/status')return json(res,{signedIn:Boolean(session(req)),aiEnabled,apkEnabled:process.env.ENABLE_APK==='1'});
 if(m==='POST' && ['/api/login','/api/register'].includes(p)){
  limit('login:'+digest(req.socket.remoteAddress||''),40,new Date().toISOString().slice(0,13));
  const {name,password}=await body(req);if(typeof name!=='string'||!/^[-a-z0-9_]{3,40}$/i.test(name)||typeof password!=='string'||password.length<12||password.length>128)fail(400,'Username 3–40 अक्षर और password 12–128 अक्षर रखें।');
  const login=name.toLowerCase();let user=q('SELECT * FROM users WHERE name=?').get(login);
  if(p==='/api/register'){if(user)fail(409,'Username उपलब्ध नहीं है।');limit('signup',100);const salt=randomBytes(16).toString('hex'),key=await hash(password,salt,64);user={id:randomUUID()};const r=q('INSERT OR IGNORE INTO users VALUES(?,?,?,?)').run(user.id,login,key.toString('hex'),salt);if(!r.changes)fail(409,'Username उपलब्ध नहीं है।')}
  else {const key=await hash(password,user?.salt||'unused-salt',64);if(!user||!timingSafeEqual(key,Buffer.from(user.password,'hex')))fail(401,'Login details जाँचें।')}
  const t=randomBytes(32).toString('hex');q('DELETE FROM sessions WHERE expires<?').run(Date.now());q('INSERT INTO sessions VALUES(?,?,?)').run(digest(t),user.id,Date.now()+7*86400000);
  res.setHeader('Set-Cookie','studio='+t+'; Path=/; HttpOnly; SameSite=Strict; Max-Age=604800'+(process.env.NODE_ENV==='production'?'; Secure':''));return json(res,{ok:true})
 }
 const uid=session(req);if(!uid)fail(401,'पहले login करें।');
 if(p==='/api/code'||p.startsWith('/api/code/'))return await api({req,res,p,m,db,uid,body,json,data});
 if(m==='POST'&&p==='/api/logout'){const token=req.headers.cookie.match(/(?:^|; *)studio=([a-f0-9]{64})/)[1];q('DELETE FROM sessions WHERE token=?').run(digest(token));res.setHeader('Set-Cookie','studio=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0');return json(res,{ok:true})}
 if(m==='GET'&&p==='/api/projects')return json(res,q('SELECT p.id,p.kind,max(v) AS version FROM projects p JOIN versions x ON p.id=x.id WHERE user=? GROUP BY p.id ORDER BY max(x.rowid) DESC').all(uid).map(r=>({...r,title:load(r.id,uid,null).spec.title})));
 if(m==='POST'&&p==='/api/projects'){
  if(reserved.has(uid))fail(409,'पहली request पूरी होने दें।');reserved.add(uid);
  try{const b=await body(req),idea=typeof b.idea==='string'?b.idea.trim():'';
  if(!idea||idea.length>4000||!['Website','Android App'].includes(b.kind))fail(400,'Project और idea जाँचें।');
  let old=b.id?load(b.id,uid,null):null;if(old&&old.v!==b.version)fail(409,'नया version खोलकर दोबारा edit करें।');
  if(old&&old.v>=100)fail(409,'Version limit reached');if(!old&&q('SELECT count(*) AS n FROM projects WHERE user=?').get(uid).n>=100)fail(409,'Project limit reached');
  const spec=b.ai?await generate(idea,old?.spec):starter(idea,old?.spec),id=old?.id||randomUUID(),v=(old?.v||0)+1;
  db.exec('BEGIN IMMEDIATE');try{if(!old)q('INSERT INTO projects VALUES(?,?,?)').run(id,uid,b.kind);q('INSERT INTO versions VALUES(?,?,?,?)').run(id,v,idea,JSON.stringify(spec));db.exec('COMMIT')}catch(e){db.exec('ROLLBACK');throw e}return json(res,load(id,uid,v))
  }finally{reserved.delete(uid)}
 }
 const match=p.match(/^\/api\/projects\/([a-f0-9-]+)(?:\/(html|zip|apk|versions))?$/);
 if(!match)fail(404,'Not found');const v=url.searchParams.has('v')?Number(url.searchParams.get('v')):null;
 if(v!==null&&(!Number.isSafeInteger(v)||v<1))fail(400,'Invalid version');
 const row=load(match[1],uid,v),format=match[2];
 if(m==='GET'&&!format)return json(res,row);
 if(m==='GET'&&format==='versions')return json(res,q('SELECT v FROM versions WHERE id=? ORDER BY v DESC').all(row.id));
 if(m==='GET'&&format==='html'){res.setHeader('Content-Disposition','attachment; filename="website.html"');res.setHeader('Content-Type','text/html; charset=utf-8');res.end(render(row.spec,row.id));return}
 if(!((m==='GET'&&format==='zip')||(m==='POST'&&format==='apk'))||row.kind!=='Android App')fail(404,'Not found');
 if(format==='apk'){if(process.env.ENABLE_APK!=='1')fail(503,'APK worker उपलब्ध नहीं है।');if(building)fail(409,'Worker busy');limit('apk',10);building=true}
 let tmp;
 try{tmp=await mkdtemp(join(tmpdir(),'studio-'));const root=join(tmp,'project');await materialize(androidFiles(row.spec,row.id,row.v),root);
 let file;if(format==='zip'){file=join(tmp,'source.zip');await run('zip',['-qr',file,'.'],{cwd:root,timeout:30000})}
 else{await run('gradle',['--no-daemon','--max-workers=2','assembleDebug'],{cwd:root,timeout:480000,maxBuffer:2000000,env:{PATH:process.env.PATH,HOME:process.env.HOME,ANDROID_HOME:process.env.ANDROID_HOME,JAVA_HOME:process.env.JAVA_HOME,GRADLE_USER_HOME:process.env.GRADLE_USER_HOME||join(process.env.HOME,'.gradle')}});file=join(root,'app/build/outputs/apk/debug/app-debug.apk')}
 res.setHeader('Content-Disposition','attachment; filename="project.'+format+'"');res.setHeader('Content-Type','application/octet-stream');res.end(await readFile(file))
 }finally{if(format==='apk')building=false;if(tmp)await rm(tmp,{recursive:true,force:true})}
 }catch(e){res.statusCode=e.status||500;json(res,{error:e.status?e.message:'Request पूरी नहीं हुई। Server configuration जाँचें।'})}}
if(process.env.STUDIO_TEST!=='1')http.createServer(handler).listen(Number(process.env.PORT)||3000,process.env.HOST||'127.0.0.1');
