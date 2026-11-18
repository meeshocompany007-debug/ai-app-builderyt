import {DatabaseSync, backup} from 'node:sqlite';
import {mkdir,access,chmod} from 'node:fs/promises';
import {dirname,resolve,join} from 'node:path';
const source=resolve(process.env.DATA_DIR||'data','studio.sqlite');
const target=resolve(process.argv[2]||join('backups',new Date().toISOString().replaceAll(':','-')+'.sqlite'));
await mkdir(dirname(target),{recursive:true,mode:0o700});
try {await access(target); throw Error('Choose a new backup filename')} catch(e) {if(e.code!=='ENOENT')throw e}
const db=new DatabaseSync(source,{readOnly:true});
try{await backup(db,target)}finally{db.close()}
await chmod(target,0o600);
const copy=new DatabaseSync(target,{readOnly:true});
try{if(copy.prepare('PRAGMA integrity_check').get().integrity_check!=='ok')throw Error('Backup failed integrity check')}finally{copy.close()}
console.log('Verified SQLite backup:',target);
