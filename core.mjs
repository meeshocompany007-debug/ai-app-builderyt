import {createHash} from 'node:crypto';
const clean=(v,n)=>typeof v==='string'?v.trim().slice(0,n):'';
export function validate(raw) {
  const title=clean(raw?.title,80), intro=clean(raw?.intro,1200);
  if (!title || !intro || !Array.isArray(raw.sections) || !raw.sections.length) throw Error('Invalid project layout');
  const sections=raw.sections.slice(0,8).map(x=>({heading:clean(x?.heading,80),body:clean(x?.body,1500)}));
  if(sections.some(x=>!x.heading || !x.body)) throw Error('Incomplete section');
  return {title,intro,sections,due:raw.due===true,priority:raw.priority===true,feature:['notes','tasks','expenses'].includes(raw.feature)?raw.feature:'none'};
}
export function starter(idea,previous) {
  const due=/due|deadline|तारीख|समयसीमा/i.test(idea),priority=/priority|priorities|प्राथमिकता/i.test(idea);
  if(previous && previous.feature==='tasks' && (due||priority))return validate({...previous,due:previous.due||due,priority:previous.priority||priority});
  const feature=/expense|खर्च|खाता/i.test(idea)?'expenses':/task|todo|टास्क|काम की सूची/i.test(idea)?'tasks':'notes';
  return validate({title:idea.split('\n')[0].slice(0,60),intro:idea,feature,due,priority,sections:[{heading:'शुरुआत',body:'नीचे अपनी पहली entry जोड़ें।'}]});
}
export const esc=s=>s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export function render(spec,id) {
  const s=validate(spec), key='studio-'+createHash('sha256').update(id).digest('hex').slice(0,24);
  const featureTitle={notes:'नोट्स',tasks:'काम की सूची',expenses:'खर्च',none:''}[s.feature];
  const script=`const key=${JSON.stringify(key)},mode=${JSON.stringify(s.feature)};const enabled=${JSON.stringify([...(s.due?['due']:[]),...(s.priority?['priority']:[])])};let entries=[];try{entries=JSON.parse(localStorage.getItem(key))||[]}catch{};if(!Array.isArray(entries))entries=[];
const list=document.querySelector('#entries'),msg=document.querySelector('#msg');
function draw(){list.replaceChildren();let total=0;entries.forEach((e,i)=>{const row=document.createElement('li');row.textContent=(e.done?'✓ ':'')+e.text+(mode==='expenses'?' · ₹'+Number(e.amount).toFixed(2):'');if(mode==='tasks'){
 for(const k of enabled){const control=document.createElement(k==='due'?'input':'select');control.setAttribute('aria-label',k==='due'?'Due date':'Priority');
 if(k==='due')control.type='date';else for(const v of ['Normal','High','Low'])control.add(new Option(v,v));
 control.value=e[k]||(k==='due'?'':'Normal');control.onchange=()=>{e[k]=control.value;save()};row.append(control)}
 }total+=Number(e.amount)||0;if(mode==='tasks'){const b=document.createElement('button');b.textContent=e.done?'फिर खोलें':'पूरा हुआ';b.onclick=()=>{e.done=!e.done;save()};row.append(b)}const del=document.createElement('button');del.textContent='हटाएँ';del.onclick=()=>{entries.splice(i,1);save()};row.append(del);list.append(row)});document.querySelector('#total').textContent=mode==='expenses'?'कुल ₹'+total.toFixed(2):''}
function save(){try{localStorage.setItem(key,JSON.stringify(entries));msg.textContent='इस डिवाइस पर सेव हुआ।'}catch{msg.textContent='यहाँ storage उपलब्ध नहीं है।'}draw()}
document.querySelector('form').onsubmit=e=>{e.preventDefault();const f=e.target;if(!f.item.value.trim())return;entries.push({text:f.item.value.trim(),amount:f.amount?Number(f.amount.value):0,due:f.due?.value||'',priority:f.priority?.value||'Normal',done:false});f.reset();save()};draw();`;
  return `<!doctype html><html lang="hi"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${esc(s.title)}</title><style>*{box-sizing:border-box}body{margin:0;color:#f7eee8;background:#17131c;font:17px/1.65 system-ui,sans-serif}main{max-width:780px;margin:auto;padding:clamp(18px,5vw,60px);overflow-wrap:anywhere}header{border-radius:20px;padding:clamp(24px,5vw,50px);background:radial-gradient(ellipse at top right,#704865,transparent 68%),#342634}h1{font:500 clamp(32px,6vw,60px)/1.15 Georgia,serif;margin:0}section{margin-top:20px;border:1px solid #4c3b50;border-radius:16px;padding:20px}h2{margin:0;font-size:22px}p{white-space:pre-wrap}button,input{font:inherit;padding:9px;border:1px solid #786779;border-radius:8px}input{width:100%;margin:5px 0 10px}button{background:#ffd2ad;cursor:pointer}li button{margin:4px;font-size:13px}</style><main><header><h1>${esc(s.title)}</h1><p>${esc(s.intro)}</p></header>${s.sections.map(x=>'<section><h2>'+esc(x.heading)+'</h2><p>'+esc(x.body)+'</p></section>').join('')}${s.feature==='none'?'':`<section><h2>${featureTitle}</h2><form><label>विवरण<input name="item" required maxlength="300"></label>${s.feature==='expenses'?'<label>राशि (₹)<input name="amount" type="number" min="0.01" max="999999999" step="0.01" required></label>':''}${s.feature==='tasks'&&s.due?'<label>Due date<input type="date" name="due"></label>':''}${s.feature==='tasks'&&s.priority?'<label>Priority<select name="priority"><option>Normal</option><option>High</option><option>Low</option></select></label>':''}<button>जोड़ें</button></form><p id="total"></p><ul id="entries"></ul><small role="status" id="msg"></small></section><script>${script}</script>`}</main></html>`;
}
