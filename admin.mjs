import {createHash,randomBytes} from 'node:crypto';
const sha = s => createHash('sha256').update(s).digest('hex');
const random = () => randomBytes(32).toString('hex');
const ownerHash = '8cc63b48d657f6452d3bcdfbdeff73529ca776a00cf113b3ca2768941a50e919';
const reject = (status,message) => { throw Object.assign(new Error(message),{status}); };
const cookie = (req,key) => (req.headers.cookie||'').split(';').map(s=>s.trim()).find(s=>s.startsWith(key+'='))?.slice(key.length+1);
const callback = () => process.env.GOOGLE_ADMIN_REDIRECT_URI;
function redirect(res,url){res.statusCode=302;res.setHeader('Location',String(url));res.end();}
export function setupAdmin(db,defaults){
 db.exec(`CREATE TABLE IF NOT EXISTS owner_login(state TEXT PRIMARY KEY, verifier TEXT, expires INTEGER);
 CREATE TABLE IF NOT EXISTS owner_sessions(token TEXT PRIMARY KEY,expires INTEGER);
 CREATE TABLE IF NOT EXISTS owner_settings(id INTEGER PRIMARY KEY,revision INTEGER,settings TEXT);`);
 const q=s=>db.prepare(s);
 q('INSERT OR IGNORE INTO owner_settings VALUES(1,0,?)').run(JSON.stringify(defaults));
 function settings(){const row=q('SELECT * FROM owner_settings WHERE id=1').get();return {...JSON.parse(row.settings),revision:row.revision};}
 const authenticated=req=>!!q('SELECT 1 FROM owner_sessions WHERE token=? AND expires>?').get(sha(cookie(req,'__Host-owner')||''),Date.now());
 async function route(req,res,url,body,json){
  const path=url.pathname;
  if(path==='/admin/signin' && req.method==='GET'){
   if(!process.env.GOOGLE_ADMIN_CLIENT_ID||!process.env.GOOGLE_ADMIN_CLIENT_SECRET||!callback()?.startsWith('https://'))reject(503,'Configure Google OAuth and the HTTPS admin callback first.');
   const state=random(),verifier=random();
   q('DELETE FROM owner_login WHERE expires<=?').run(Date.now());
   q('INSERT INTO owner_login VALUES(?,?,?)').run(sha(state),verifier,Date.now()+600000);
   res.setHeader('Set-Cookie','__Host-flow='+state+'; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=600');
   const dest=new URL('https://accounts.google.com/o/oauth2/v2/auth');
   dest.search=new URLSearchParams({client_id:process.env.GOOGLE_ADMIN_CLIENT_ID,redirect_uri:callback(),scope:'openid email',response_type:'code',state,code_challenge:createHash('sha256').update(verifier).digest('base64url'),code_challenge_method:'S256',prompt:'select_account'});
   return redirect(res,dest);
  }
  if(path==='/admin/callback' && req.method==='GET'){
   const state=url.searchParams.get('state'),code=url.searchParams.get('code');
   if(!state||!code||state!==cookie(req,'__Host-flow'))reject(403,'Invalid sign-in request.');
   const flow=q('DELETE FROM owner_login WHERE state=? AND expires>? RETURNING verifier').get(sha(state),Date.now());
   if(!flow)reject(403,'Expired sign-in request.');
   const token=await fetch('https://oauth2.googleapis.com/token',{method:'POST',signal:AbortSignal.timeout(15000),body:new URLSearchParams({client_id:process.env.GOOGLE_ADMIN_CLIENT_ID,client_secret:process.env.GOOGLE_ADMIN_CLIENT_SECRET,redirect_uri:callback(),grant_type:'authorization_code',code,code_verifier:flow.verifier})});
   if(!token.ok)reject(403,'Google sign-in failed.');
   const access=(await token.json()).access_token;
   if(!access)reject(403,'Google token missing.');
   const profile=await fetch('https://openidconnect.googleapis.com/v1/userinfo',{headers:{Authorization:'Bearer '+access},signal:AbortSignal.timeout(15000)});
   if(!profile.ok)reject(403,'Google verification failed.');
   const identity=await profile.json();
   if(identity.email_verified!==true||!identity.sub||sha(String(identity.email).toLowerCase())!==ownerHash)reject(403,'This account has no administrator access.');
   const session=random();q('DELETE FROM owner_sessions WHERE expires<=?').run(Date.now());
   q('INSERT INTO owner_sessions VALUES(?,?)').run(sha(session),Date.now()+3600000);
   res.setHeader('Set-Cookie',['__Host-owner='+session+'; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=3600','__Host-flow=; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=0']);
   return redirect(res,'/admin');
  }
  if(!authenticated(req))reject(403,'Sign in with the administrator Google account.');
  if(req.method==='POST' && (!callback()||req.headers.origin!==new URL(callback()).origin))reject(403,'Invalid admin origin.');
  if(path==='/api/admin/logout'&&req.method==='POST'){
   q('DELETE FROM owner_sessions WHERE token=?').run(sha(cookie(req,'__Host-owner')));
   res.setHeader('Set-Cookie','__Host-owner=; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=0');return json(res,{ok:true});
  }
  if(path!=='/api/admin/settings')reject(404,'Unknown admin action.');
  if(req.method==='GET')return json(res,settings());
  if(req.method!=='POST')reject(405,'Unsupported method.');
  const s=await body(req);
  if(!Array.isArray(s.tools)||s.tools.length>30||!Number.isSafeInteger(s.dailyLimit)||s.dailyLimit<0||s.dailyLimit>100000||typeof s.ai!=='boolean'||typeof s.apk!=='boolean')reject(400,'Check tools and limits.');
  const tools=s.tools.map(t=>{
   if(!t||!['gemini','openai'].includes(t.provider)||typeof t.name!=='string'||!t.name.trim()||t.name.length>60||typeof t.model!=='string'||!(/^[a-zA-Z0-9._-]{1,100}$/).test(t.model)||typeof t.enabled!=='boolean')reject(400,'Check tool name, provider and model.');
   return {name:t.name.trim(),provider:t.provider,model:t.model,enabled:t.enabled};
  });
  if(new Set(tools.map(t=>t.name)).size!==tools.length)reject(400,'Use unique tool names.');
  for(const key of ['fast','advanced'])if(typeof s[key]!=='string'||(s[key]&&!tools.some(t=>t.name===s[key]&&t.enabled))||(s.ai&&!s[key]))reject(400,'Choose an enabled tool for each active route.');
  const updated={tools,fast:s.fast,advanced:s.advanced,ai:s.ai,apk:s.apk,dailyLimit:s.dailyLimit};
  if(!Number.isSafeInteger(s.revision)||!q('UPDATE owner_settings SET settings=?,revision=revision+1 WHERE id=1 AND revision=?').run(JSON.stringify(updated),s.revision).changes)reject(409,'Settings changed in another tab. Reload first.');
  return json(res,settings());
 }
 return {settings,route};
}
