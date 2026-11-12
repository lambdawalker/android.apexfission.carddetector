import test from 'node:test';
import assert from 'node:assert/strict';
import {modelPin, modelPinNotice} from '../model-pin.mjs';
test('legacy model records retain the exact confirmed detector pin', () => {
 assert.equal(modelPin({module:'tfmodel',group:'com.apexfission.android.carddetector',core_version:'0.1.0'}), 'com.apexfission.android.carddetector:core:0.1.0');
});
test('CLI model records use resolved verification facts, not current properties', () => {
 const record={module:'tfmodel',group:'com.github.lambdawalker',verification:{required_api_dependencies:[{group:'com.apexfission.android.carddetector',artifact:'card-detector',version:'0.2.0'}]}};
 assert.equal(modelPin(record),'com.apexfission.android.carddetector:card-detector:0.2.0');
 assert.match(modelPinNotice({module:'tfmodel',destinations:{jitpack:record}},'es'), /card-detector:0.2.0/);
});
test('missing or unsafe model pins stop documentation generation', () => {
 assert.throws(()=>modelPin({module:'tfmodel',group:'example'}));
 assert.throws(()=>modelPin({module:'tfmodel',group:'example',core_version:'`unsafe`'}));
 assert.equal(modelPin({module:'carddetector'}),null);
});
