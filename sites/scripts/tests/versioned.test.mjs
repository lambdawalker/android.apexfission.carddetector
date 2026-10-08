import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,writeFileSync,rmSync,readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {readRevision,readTranslationTree,translatedGuide,rewriteVersioned,modelReference,modelPrerequisite} from '../versioned.mjs';
import {catalogLinks} from '../navigation.mjs';
import {base,repository} from '../markdown.mjs';
test('reads pinned Git documentation after the working file changes; refuses unknown revision',()=>{
 const root=mkdtempSync(join(tmpdir(),'docs-history-'));
 const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8'}).trim();
 try{
  git('init','-q');git('config','user.name','Test');git('config','user.email','test@example.invalid');
  mkdirSync(join(root,'docs/agents'),{recursive:true});writeFileSync(join(root,'docs/agents/index.md'),'# Old API\n');
  git('add','.');git('commit','-qm','first');const sha=git('rev-parse','HEAD');
  writeFileSync(join(root,'docs/agents/index.md'),'# New API\n');
  assert.equal(readRevision(root,sha).get('docs/agents/index.md'),'# Old API\n');
  assert.throws(()=>readRevision(root,'f'.repeat(40)),/revision/i);
  assert.throws(()=>readRevision(root,'main'),/SHA/);
 }finally{rmSync(root,{recursive:true,force:true});}
});
test('missing or stale translations fall back to same-source English',()=>{
 const en='# Ownership\n\nKeep bitmap alive.\n',es='# Propiedad\n\nConserva el bitmap.\n';
 const files=new Map([['docs/agents/concepts.md',en],['docs/es/concepts.md',es]]);
 assert.equal(translatedGuide(files,'concepts.md','es').fallback,true);
 files.set('docs/es/translations.json',JSON.stringify({'concepts.md':createHash('sha256').update(en).digest('hex')}));
 assert.deepEqual(translatedGuide(files,'concepts.md','es'),{markdown:es,fallback:false});
 files.set('docs/agents/concepts.md',en+'Changed.');
 assert.equal(translatedGuide(files,'concepts.md','es').markdown,en+'Changed.');
});
test('links and raw guides remain version scoped, source/media links pinned, code untouched',()=>{
 const context={language:'es',module:'carddetector',version:'0.1.2',ref:'a'.repeat(40),source:'b'.repeat(40),pages:new Set(['concepts.md','api/detection.md'])};
 const input='[Ownership](../concepts.md#ownership-table) [Install](../../../IMPORT.md) [Source](../../../carddetector/src/Foo.kt)\n\n```text\n[unchanged](../concepts.md)\n```';
 const out=rewriteVersioned(input,'docs/agents/api/detection.md',context);
 assert.ok(out.includes(`${base}/es/carddetector/0.1.2/concepts/#ownership-table`));
 assert.ok(out.includes(`${base}/es/carddetector/0.1.2/installation/`));
 assert.ok(out.includes(`${repository}/blob/${context.source}/carddetector/src/Foo.kt`));
 assert.ok(out.includes('[unchanged](../concepts.md)'));
 const raw=rewriteVersioned(input,'docs/agents/api/detection.md',context,'raw');
 assert.ok(raw.includes(`${base}/es/carddetector/0.1.2/raw/concepts.md#ownership-table`));
});
test('model reference excludes sibling detector declarations',()=>{
 const md='# API\n\n### Core (carddetector)\n\n```kotlin\nclass Detector\n```\n\n### Catalog (tfmodel)\n\n```kotlin\nval ModelCatalog.path: String\n```\n';
 const out=modelReference(md,'en');
 assert.ok(out.includes('val ModelCatalog.path'));assert.ok(!out.includes('class Detector'));
});

test('fresh Spanish translations preserve original code',()=>{
 const root=new URL('../../../',import.meta.url);
 const hashes=JSON.parse(readFileSync(new URL('docs/es/translations.json',root),'utf8'));
 for(const [guide,hash] of Object.entries(hashes)) {
  const en=readFileSync(new URL('docs/agents/'+guide,root),'utf8');
  const es=readFileSync(new URL('docs/es/'+guide,root),'utf8');
  assert.match(hash,/^[0-9a-f]{64}$/,guide+' invalid source hash');
  if(createHash('sha256').update(en).digest('hex')!==hash)continue; // Stale translations intentionally fall back to English.
  assert.deepEqual(es.match(/```[\s\S]*?```/g),en.match(/```[\s\S]*?```/g),guide+' modified code');
 }
 assert.ok(Object.keys(hashes).length>0);
});

test('translation tree remains readable after squash-style commit replacement',()=>{
 const root=mkdtempSync(join(tmpdir(),'docs-translation-tree-'));
 const git=(...args)=>execFileSync('git',args,{cwd:root,encoding:'utf8'}).trim();
 try {
  git('init','-q');git('config','user.name','Test');git('config','user.email','test@example.invalid');
  mkdirSync(join(root,'docs/es'),{recursive:true});writeFileSync(join(root,'docs/es/index.md'),'# Español\n');
  git('add','.');git('commit','-qm','translation');const tree=git('rev-parse','HEAD:docs/es');
  // A squash keeps content trees but gives the containing commit a different SHA.
  git('commit','--amend','-qm','squashed');
  assert.equal(readTranslationTree(root,tree).get('docs/es/index.md'),'# Español\n');
  assert.throws(()=>readTranslationTree(root,'f'.repeat(40)),/translation tree/i);
 }finally{rmSync(root,{recursive:true,force:true});}
});

test('bundled-model quickstart directs installation to independently versioned model choices',()=>{
 const context={language:'es',module:'carddetector',version:'0.1.2',ref:'a'.repeat(40),pages:new Set(['quickstart.md'])};
 const out=rewriteVersioned('[modelo](../../IMPORT.md)','docs/agents/quickstart.md',context);
 assert.ok(out.includes(`${base}/es/#tfmodel`));
 assert.ok(!out.includes('/carddetector/0.1.2/installation/'));
 const notice=modelPrerequisite('es');
 assert.match(notice,/tfmodel/);assert.match(notice,/fijad/);assert.match(notice,/JitPack/);
 assert.match(notice,/no incluye/);
});

test('catalog navigation preserves language and offers module release scopes',()=>{
 const scopes=[{language:'en',module:'carddetector',version:'0.1.2',root:`${base}/en/carddetector/0.1.2/`},
  {language:'es',module:'carddetector',version:'0.1.2',root:`${base}/es/carddetector/0.1.2/`},
  {language:'es',module:'tfmodel',version:'development',root:`${base}/es/tfmodel/development/`}];
 const entries=catalogLinks(scopes,'es');
 assert.equal(entries.length,2);
 assert.ok(entries.every(e=>e.href.startsWith(`${base}/es/`)));
 assert.ok(entries.some(e=>e.title.includes('Desarrollo')));
});
