import assert from 'node:assert/strict';
import {readFile,stat,readdir} from 'node:fs/promises';
import {resolve} from 'node:path';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {modelPinNotice} from '../model-pin.mjs';
const root=fileURLToPath(new URL('../../../',import.meta.url)),dist=resolve(root,'sites/dist');
const scopes=JSON.parse(await readFile(resolve(dist,'versions.json'),'utf8'));
const base='/android.apexfission.carddetector';
const file=href=>resolve(dist,href.slice(base.length).replace(/^\//,''));
for(const scope of scopes){
 for(const page of scope.pages){const html=await readFile(resolve(file(page.href),'index.html'),'utf8');await readFile(file(page.raw));assert.ok(html.includes('data-pagefind-filter'),page.href+' scope search');}
 const install=await readFile(file(scope.pages.find(page=>page.slug==='installation').raw),'utf8');
 if(scope.module==='tfmodel'){
  assert.ok(scope.pages.every(page=>['','index','installation','model-contract','api','api/detection','overview'].includes(page.slug)||page.slug.startsWith('project/')));
  const index=await readFile(file(scope.pages.find(page=>page.slug==='').raw),'utf8');assert.match(index,/version is independent|versión es independiente/);
  const api=await readFile(file(scope.pages.find(page=>page.slug==='api/detection').raw),'utf8');assert.match(api,/tfmodel\//);assert.doesNotMatch(api,/^## `carddetector\//m);
  if(!scope.development){assert.ok(scope.release);for(const line of modelPinNotice(scope.release,scope.language).trim().split('\n').filter(Boolean))assert.ok(install.includes(line),scope.version+' immutable pin');}
 }else{
  const quick=scope.pages.find(page=>page.slug==='quickstart');if(quick){const raw=await readFile(file(quick.raw),'utf8');assert.match(raw,/optional model package|modelo opcional/);assert.match(raw,/\/tfmodel\/[^/]+\/(?:raw\/installation.md|installation\/)/);assert.match(raw,/share one artifact ID|mismo identificador de artefacto/);}
 }
 const overview=await readFile(resolve(file(scope.root+'overview/'),'index.html'),'utf8');assert.match(overview,/synthetic host-rendered|vistas sintéticas/);assert.match(overview,/<img[^>]+screenshots/);assert.match(overview,/do not verify features|no verifican funciones/);
 const media=await readFile(resolve(file(scope.root+'project/media/'),'index.html'),'utf8');assert.match(media,/<video/);assert.match(media,/Capture source and device unknown/);assert.ok(media.includes(scope.source.slice(0,12)));
}
for(const alias of ['agents','getting-started','reference','task-recipes','installation','development','coverage','releases','media']){const html=await readFile(resolve(dist,alias,'index.html'),'utf8');assert.match(html,/carddetector\/development\//,alias+' current route');assert.ok(html.includes('location.hash'),alias+' preserves incoming fragments');}
const styles=(await Promise.all((await readdir(resolve(dist,'_astro'))).filter(name=>name.endsWith('.css')).map(name=>readFile(resolve(dist,'_astro',name),'utf8')))).join('\n');assert.ok(styles.includes('.scope-banner'),'shared navigation CSS retained alongside brand CSS');assert.ok(styles.includes('#087e8b'),'consumer brand CSS retained');
const rootRaw=await readFile(resolve(dist,'agents/index.md'),'utf8');assert.match(rootRaw,/Module: carddetector/);assert.match(rootRaw,/Version: development/);
const video=await readFile(resolve(dist,'media/card-detection.mp4'));assert.ok(video.length>1000000);assert.equal(createHash('sha256').update(video).digest('hex'),createHash('sha256').update(await readFile(resolve(root,'docs/demo.mp4'))).digest('hex'));
for(const name of ['tracking.png','capture-front-en.png','capture-front-es.png'])assert.ok((await stat(resolve(dist,'screenshots',name))).size>1000);
console.log(`Verified ${scopes.length} module/language/history scopes: independent pins/API, raw routes, development entry, showcase and immutable recording.`);
