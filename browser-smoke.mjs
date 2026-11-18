// Run on a CI host with Google Chrome and Node 24.
import {spawn} from 'node:child_process';
import {mkdtemp,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import assert from 'node:assert/strict';
import {setTimeout as delay} from 'node:timers/promises';
const dir=await mkdtemp(join(tmpdir(),'studio-browser-'));
const api=spawn(process.execPath,['server.mjs'],{env:{...process.env,PORT:'3100',DATA_DIR:join(dir,'data')},stdio:'inherit'});
const chrome=spawn('google-chrome',['--headless','--no-sandbox','--disable-dev-shm-usage','--remote-debugging-port=9223','--user-data-dir='+join(dir,'profile'),'about:blank'],{stdio:'ignore'});
let ws;
try{
 let tabs;
 for(let i=0;i<80;i++){try{tabs=await(await fetch('http://127.0.0.1:9223/json/list')).json();if(tabs.some(x=>x.type==='page'))break}catch{}await delay(200)}
 ws=new WebSocket(tabs.find(x=>x.type==='page').webSocketDebuggerUrl);await new Promise((ok,no)=>{ws.onopen=ok;ws.onerror=no});
 let seq=0;const calls=new Map();ws.onmessage=e=>{const m=JSON.parse(e.data);if(!calls.has(m.id))return;const resolve=calls.get(m.id);calls.delete(m.id);resolve(m)};
 const send=(method,params={})=>new Promise(resolve=>{calls.set(++seq,resolve);ws.send(JSON.stringify({id:seq,method,params}))});
 const js=async expression=>{const r=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.error||r.result.exceptionDetails)throw Error(JSON.stringify(r));return r.result.result.value};
 const wait=async expr=>{for(let n=0;n<80;n++){if(await js(expr))return;await delay(100)}throw Error('Timed out: '+expr+' '+await js('document.body.innerText.slice(-500)'))};
 await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});
 await send('Page.navigate',{url:'http://127.0.0.1:3100/studio'});await wait('document.querySelector("#auth")?.hidden===false');
 await js("document.querySelector('[name=name]').value='mobiletest';document.querySelector('[name=password]').value='sample-password-123';document.querySelector('[value=register]').click()");
 await wait("document.querySelector('#workspace')?.hidden===false");
 await js("document.querySelector('#kind').value='Android App';document.querySelector('#prompt').value='मेरी खर्च डायरी';document.querySelector('[value=starter]').click()");
 await wait("document.querySelector('#result').hidden===false");
 assert.equal(await js("document.querySelector('#name').textContent"),'मेरी खर्च डायरी');
 assert.equal(await js('document.documentElement.scrollWidth <= innerWidth'),true);
 const exported=await js("fetch(document.querySelector('#html').href).then(x=>x.text())");
 const result=await fetch('http://127.0.0.1:3100/api/status');assert.equal(result.status,200);
 // Open the exported document with an isolated browser origin for its local entries.
 const {writeFile}=await import('node:fs/promises');const page=join(dir,'expense.html');await writeFile(page,exported);
 await send('Page.navigate',{url:'file://'+page});await wait("document.querySelector('input[name=amount]')");
 await js("document.querySelector('[name=item]').value='चाय';document.querySelector('[name=amount]').value='20';document.querySelector('form button').click()");
 await wait("document.querySelector('#total').textContent.includes('20.00')");
 await send('Page.reload');await wait("document.querySelector('#entries')?.textContent.includes('चाय')");
 console.log('Mobile viewport, registration, project creation, export and persisted expense passed');
} finally {ws?.close();chrome.kill();api.kill();await delay(350);await rm(dir,{recursive:true,force:true,maxRetries:5})}
