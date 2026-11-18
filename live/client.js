const $=x=>document.getElementById(x);
const shelf='aibuilder.projects.v1';
const message=t=>$('status').textContent=t;
function encode(value){const bytes=new TextEncoder().encode(JSON.stringify(value));return btoa(Array.from(bytes,x=>String.fromCharCode(x)).join('')).replaceAll('+','-').replaceAll('/','_').replace(/=+$/,'')}
function decode(text){if(text.length>24000)throw Error('Link बहुत लंबा है।');return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(text.replaceAll('-','+').replaceAll('_','/')),c=>c.charCodeAt(0))))}
const shared=location.hash.startsWith('#app=');
if(shared){
 try{const {id,spec}=decode(location.hash.slice(5));if(!/^[a-f0-9-]{36}$/.test(id))throw Error();const page=render(validate(spec),id);document.open();document.write(page);document.close();window.addEventListener('hashchange',()=>location.reload())}catch{document.querySelector('main').textContent='यह share link अधूरा है। पूरा link दोबारा खोलें।'}
}else{
 window.addEventListener('hashchange',()=>location.reload());
 let projects={},active=null,version=0;
 function read(){return JSON.parse(localStorage.getItem(shelf)||'{}')}
 try{projects=read()}catch{message('इस browser का project storage उपलब्ध नहीं है।')}
 function menu(){const select=$('saved');select.replaceChildren(new Option('नया project',''));for(const p of Object.values(projects))select.add(new Option(p.versions.at(-1).title,p.id));select.value=active||''}
 function persist(spec, copy = false) {
  const latest = read();
  const source = projects[active];
  if (source && latest[active]?.versions.length !== source.versions.length)
   throw Error('दूसरे tab में बदलाव हुआ। Reload करके दोबारा खोलो।');
  const existing = copy ? null : source;
  if (!existing && Object.keys(latest).length >= 100) throw Error('100 projects की सीमा पूरी हुई।');
  if (existing?.versions.length >= 100) throw Error('100 versions की सीमा पूरी हुई।');
  const id = existing?.id || crypto.randomUUID();
  const versions = [...(existing?.versions || []), validate(spec)];
  const updated = {...latest, [id]: {id, kind: source?.kind || $('platform').value, versions}};
  localStorage.setItem(shelf, JSON.stringify(updated));
  projects = updated;
  active = id;
  version = versions.length - 1;
  menu();
  show();
 }
 function selected(){return projects[active].versions[version]}
 function fields(spec){
  return [
   [spec, 'title', 'नाम', 80], [spec, 'intro', 'परिचय', 1200],
   ...spec.sections.flatMap((section, index) => [
    [section, 'heading', 'सेक्शन '+(index+1)+' · शीर्षक', 80],
    [section, 'body', 'सेक्शन '+(index+1)+' · विवरण', 1500]
   ])
  ];
 }
 function loadField(){
  const [container,key,,limit] = fields(selected())[Number($('field').value)];
  $('copyText').maxLength = limit;
  $('copyText').value = container[key];
 }
 function change(action, success){
  try { action(); message(success); } catch (error) { message(error.message); }
 }
 $('width').onchange = () => { $('frame').style.width = $('width').value; };
 $('field').onchange = () => { loadField(); $('frame').srcdoc = render(selected(), active); };
 $('copyText').oninput = () => {
  const draft = structuredClone(selected());
  const [container,key] = fields(draft)[Number($('field').value)];
  const value = $('copyText').value.trim();
  if (value) { container[key] = value; $('frame').srcdoc = render(draft, active); }
 };
 $('editor').ontoggle = () => {
  if (!$('editor').open && active) { loadField(); $('frame').srcdoc = render(selected(), active); }
 };
 $('saveText').onclick = () => change(() => {
  const draft = structuredClone(selected());
  const [container,key] = fields(draft)[Number($('field').value)];
  const value = $('copyText').value.trim();
  if (!value) throw Error('टेक्स्ट लिखो।');
  container[key] = value;
  persist(draft);
 }, 'बदला हुआ टेक्स्ट नए version में सेव हुआ।');
 $('restore').onclick = () => change(() => persist(selected()), 'चुना हुआ version नई revision में सेव हुआ।');
 $('fork').onclick = () => change(() => {
  const spec = selected();
  persist({...spec, title:spec.title.slice(0,74)+' (copy)'}, true);
 }, 'अलग project बन गया।');
 function show(){
  const p=projects[active],s=p?.versions[version];$('output').hidden=!s;$('platform').disabled=!!s;$('share').hidden=true;$('build').textContent=s?'बदलाव सेव करो':'Project बनाओ';
  if(!s){$('title').value=$('prompt').value='';$('frame').srcdoc='';return}
  $('editor').open=false;
  $('field').replaceChildren(...fields(s).map((field,index)=>new Option(field[2],index)));
  loadField(); $('restore').disabled=version===p.versions.length-1;
  $('title').value=s.title;$('platform').value=p.kind;$('prompt').value='';$('version').replaceChildren(...p.versions.map((_,i)=>new Option('Version '+(i+1),i)));$('version').value=version;$('source').hidden=p.kind!=='Android App';$('frame').srcdoc=render(s,p.id)
 }
 async function download(data,name){if(location.protocol==='file:'&&window.BuilderFiles){
 const chunks=new Uint8Array(await data.arrayBuffer());let raw='';for(const byte of chunks)raw+=String.fromCharCode(byte);window.BuilderFiles.save(name,btoa(raw));return}const url=URL.createObjectURL(data),a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),60000)}
 menu();show();
 $('saved').onchange=()=>{active=$('saved').value||null;version=active?projects[active].versions.length-1:0;show();message('')};
 $('version').onchange=()=>{version=Number($('version').value);show()};
 $('build').onclick=()=>{try{
  const idea=$('prompt').value.trim(),title=$('title').value.trim(),p=projects[active];
  if(!idea&&!title)throw Error('नाम या idea लिखो।');
  const old=p?.versions[version];
  let s;
  if(!old)s=starter(idea||title);
  else if(!idea)s={...old};
  else if(old.feature==='tasks'&&/due|deadline|तारीख|समयसीमा|priority|priorities|प्राथमिकता/i.test(idea))s=starter(idea,old);
  else throw Error('इस mode में नाम बदलें या task में due date / priority जोड़ें।');
  if(title)s.title=title;
  persist(s);message('Version '+(version+1)+' सेव हुआ।');$('output').scrollIntoView({behavior:'smooth'})
 }catch(e){message(e.message)}};
 $('publish').onclick=()=>{const p=projects[active];const url=new URL(location.href);if(url.protocol==='file:'){url.href=LIVE_URL}url.hash='app='+encode({id:p.id,spec:p.versions[version]});$('url').value=url.href;$('visit').href=url.href;$('share').hidden=false;message('लिंक तैयार है। इसमें इस version का डिज़ाइन शामिल है।')};
 $('copy').onclick=async()=>{try{await navigator.clipboard.writeText($('url').value);message('Link copy हुआ।')}catch{$('url').select();message('Link select हो गया। Copy दबाएँ।')}};
 $('export').onclick=()=>download(new Blob([render(projects[active].versions[version],active)],{type:'text/html;charset=utf-8'}),'my-project.html');
 $('source').onclick=()=>download(projectZip(androidFiles(projects[active].versions[version],active,version+1)),'Android-source.zip');
}
