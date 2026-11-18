import {androidFiles,materialize} from './android.mjs';
import {starter} from './core.mjs';
const sample=starter('मेरी खर्च डायरी');
sample.intro='अपने रोज़ के खर्च लिखें और कुल देखें।';
await materialize(androidFiles(sample,'studio-demo-expenses'), 'build/sample');
