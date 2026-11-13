import test from 'node:test';
import assert from 'node:assert/strict';
import {modelReference,modelOverview,modelPrerequisite,insertGuideNotice} from '../projection.mjs';
import {showcase} from '../showcase.mjs';
const scope={language:'en',module:'tfmodel',root:'/repo/en/tfmodel/0.2.0/'};
test('model API retains current file sections and legacy declaration sections only',()=>{
 const result=modelReference('# API\n\n## `carddetector/src/Detector.kt`\n```kotlin\ninterface Detector\n```\n\n## `tfmodel/src/add.kt` {#stable}\n[Source](url)\n```kotlin\nfun add()\n```\n\n## `carddetector/src/Other.kt`\nclass Other');
 assert.match(result,/fun add/);assert.match(result,/#stable/);assert.doesNotMatch(result,/interface Detector|class Other/);
 assert.match(modelReference('### add (tfmodel)\nfun add()\n### detector (core)\nclass Detector'),/fun add/);
 assert.throws(()=>modelReference('## `carddetector/Only.kt`\nclass Only'));
});
test('model overview/prerequisite keeps independent release selection and JitPack warning',()=>{
 assert.match(modelOverview(scope),/version is independent/);
 const notice=modelPrerequisite({...scope,module:'carddetector'},'/repo/en/tfmodel/0.1.6/installation/');
 assert.match(notice,/tfmodel\/0\.1\.6\/installation/);assert.match(notice,/share one artifact ID/);
});
test('showcase uses Markdown media and retains synthetic illustration disclaimer',()=>{
 const text=showcase(scope,'/repo');assert.match(text,/synthetic host-rendered/);assert.match(text,/do not verify features/);assert.match(text,/!\[1\. Front\]\(\/repo\/screenshots\/capture-front-en.png\)/);assert.doesNotMatch(text,/<img|<figure|<div/);
});

test('raw quickstart keeps optional-model notice after a provenance preamble',()=>{
 const markdown='> Module: carddetector · Version: development\n\n# Quickstart\n\nCode below.\n';
 const result=insertGuideNotice(markdown,modelPrerequisite({...scope,module:'carddetector'},'/repo/en/tfmodel/0.1.2/installation/'));
 assert.match(result,/> Module: carddetector/);assert.match(result,/# Quickstart\n\n> \*\*This example requires the optional model package/);assert.match(result,/Code below/);
});
