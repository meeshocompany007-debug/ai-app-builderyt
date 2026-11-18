const $=id=>document.getElementById(id);
let state=null, caps={}, latest=0;
async function api(path,body){
 const options=body===undefined?{}:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)};
 const r=await fetch(path,options);if(!r.ok)throw Error((await r.json()).error);
 return r;
}
function listen(id,event,handler){$(id).addEventListener(event,async e=>{
 e.preventDefault();const button=e.submitter||((e.target.tagName==='BUTTON')?e.target:null);
 if(button)button.disabled=true;
 try{await handler(e)}catch(err){$('msg').textContent=err.message}
 finally{if(button)button.disabled=false;if(id==='editor')$('ai').disabled=!caps.aiEnabled}
})}
function fresh(){state=null;latest=0;$('editor').reset();$('kind').disabled=false;$('result').hidden=true;$('preview').srcdoc='';$('msg').textContent=''}
async function refresh(){
 caps=await(await api('/api/status')).json();$('auth').hidden=caps.signedIn;$('workspace').hidden=!caps.signedIn;$('logout').hidden=!caps.signedIn;
 $('ai').disabled=!caps.aiEnabled;$('availability').textContent=caps.aiEnabled?'AI उपलब्ध है।':'Starter उपलब्ध है। AI के लिए server configuration बाकी है।';
 if(caps.signedIn)await projects()
}
async function projects(){
 const rows=await(await api('/api/projects')).json();$('list').replaceChildren();
 if(!rows.length)$('list').textContent='पहला project नीचे बनाओ।';
 for(const row of rows){const b=document.createElement('button');b.className='soft';b.textContent=row.title;b.onclick=()=>open(row.id).catch(e=>$('msg').textContent=e.message);$('list').append(b)}
}
async function open(id,v){
 const root='/api/projects/'+id;const row=await(await api(root+(v?'?v='+v:''))).json();
 const versions=await(await api(root+'/versions')).json();const href=root+'/html?v='+row.v;
 const page=await(await api(href)).text();state=row;latest=versions[0].v;
 $('versions').replaceChildren(...versions.map(x=>new Option('Version '+x.v,x.v)));$('versions').value=row.v;
 $('kind').value=row.kind;$('kind').disabled=true;$('prompt').value=row.idea;$('name').textContent=row.spec.title;
 $('preview').srcdoc=page;$('html').href=href;$('zip').href=root+'/zip?v='+row.v;
 $('zip').hidden=$('apk').hidden=row.kind!=='Android App';$('apk').disabled=!caps.apkEnabled;
 $('result').hidden=false;
}
listen('login','submit',async e=>{const body=Object.fromEntries(new FormData(e.target));await api('/api/'+e.submitter.value,body);e.target.reset();$('msg').textContent='';await refresh()});
listen('logout','click',async()=>{await api('/api/logout',{});fresh();await refresh()});
listen('fresh','click',fresh);
listen('versions','change',()=>open(state.id,$('versions').value));
listen('editor','submit',async e=>{
 if(state && state.v!==latest)throw Error('बदलाव के लिए सबसे नया version चुनो।');
 $('msg').textContent='Project तैयार हो रहा है…';
 const row=await(await api('/api/projects',{id:state?.id,version:state?.v,kind:$('kind').value,idea:$('prompt').value,ai:e.submitter.value==='ai'})).json();
 await open(row.id);await projects();$('msg').textContent='Version '+row.v+' सेव हुआ।';$('result').scrollIntoView()
});
listen('apk','click',async()=>{const id=state.id,v=state.v;$('msg').textContent='APK build चल रहा है…';const r=await api('/api/projects/'+id+'/apk?v='+v,{});
 const url=URL.createObjectURL(await r.blob());const a=document.createElement('a');a.href=url;a.download='project-v'+v+'.apk';a.click();setTimeout(()=>URL.revokeObjectURL(url),60000);$('msg').textContent='APK डाउनलोड तैयार है।'});
refresh().catch(e=>$('msg').textContent=e.message);
