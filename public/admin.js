const el=id=>document.getElementById(id);
let draft;
async function request(path,body){
 const response=await fetch('/api/admin/'+path,body===undefined?{}:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
 const value=await response.json();
 if(!response.ok)throw Error(value.error);
 return value;
}
function routes(){
 for(const key of ['fast','advanced']){
  const select=el(key);
  select.replaceChildren(new Option('Choose tool',''),...draft.tools.filter(t=>t.enabled&&t.name).map(t=>new Option(t.name,t.name)));
  select.value=draft[key];draft[key]=select.value;
  select.onchange=()=>draft[key]=select.value;
 }
}
function render(){
 el('tools').replaceChildren();
 for(const tool of draft.tools){
  const group=document.createElement('fieldset');
  for(const key of ['name','provider','model','enabled']){
   const label=document.createElement('label');
   label.textContent=({name:'Tool name ',provider:'Provider ',model:'Model ID ',enabled:'Enabled '})[key];
   const field=document.createElement(key==='provider'?'select':'input');
   if(key==='provider')field.append(new Option('Google Gemini','gemini'),new Option('OpenAI','openai'));
   if(key==='enabled'){field.type='checkbox';field.checked=tool.enabled;}else{field.value=tool[key];field.required=true;}
   field.onchange=()=>{
    const old=tool[key];tool[key]=key==='enabled'?field.checked:field.value.trim();
    if(key==='name'&&old)for(const role of ['fast','advanced'])if(draft[role]===old)draft[role]=tool.name;
    routes();
   };
   label.append(field);group.append(label);
  }
  const remove=document.createElement('button');remove.type='button';remove.textContent='Remove tool';
  remove.onclick=()=>{draft.tools=draft.tools.filter(t=>t!==tool);render();};
  group.append(remove);el('tools').append(group);
 }
 routes();
}
function show(data){
 draft=data;el('login').hidden=true;el('form').hidden=false;
 el('ai').checked=data.ai;el('apk').checked=data.apk;el('dailyLimit').value=data.dailyLimit;render();
}
el('add').onclick=()=>{draft.tools.push({name:'',provider:'gemini',model:'',enabled:true});render();};
el('form').onsubmit=async event=>{
 event.preventDefault();
 try{show(await request('settings',{...draft,ai:el('ai').checked,apk:el('apk').checked,dailyLimit:Number(el('dailyLimit').value)}));el('status').textContent='Settings saved.';}catch(error){el('status').textContent=error.message;}
};
el('logout').onclick=async()=>{try{await request('logout',{});location.reload();}catch(error){el('status').textContent=error.message;}};
request('settings').then(show).catch(error=>el('status').textContent=error.message);
