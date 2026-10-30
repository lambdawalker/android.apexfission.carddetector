import test from 'node:test';
import assert from 'node:assert/strict';
import {latestScope, navigationView, sidebarGroups, destination} from '../navigation.mjs';
import {base} from '../markdown.mjs';
const pages=['overview','installation','','quickstart','model-contract','configuration','concepts','recipes','demos','api','api/components','api/detection','api/overlays','api/adapters','api/utilities','limitations','troubleshooting','migration'].map(slug=>({slug,title:'Content title '+slug}));
const scopes=['en','es'].flatMap(language=>[
 ...['0.9.0','0.10.0','development'].map(version=>({language,module:'carddetector',version,root:`${base}/${language}/carddetector/${version}/`,pages})),
 {language,module:'tfmodel',version:'0.1.2',root:`${base}/${language}/tfmodel/0.1.2/`,pages:pages.filter(p=>['overview','installation','','model-contract','api/detection'].includes(p.slug))},
]);
test('default is the highest confirmed semantic release, never development',()=>{
 assert.equal(latestScope(scopes,'en','carddetector').version,'0.10.0');
 const view=navigationView(scopes,base+'/');
 assert.equal(view.scope.version,'0.10.0');
 assert.equal(view.slug,'overview');
});
test('unknown and error pages offer a valid overview rather than a nonexistent raw guide',()=>{
 const view=navigationView(scopes,base+'/404/');
 assert.equal(view.slug,'overview');
 assert.equal(destination(view.scope,view),view.scope.root+'overview/');
});
test('missing archived content is labeled with its development destination',()=>{
 const partial=scopes.map(s=>s.version==='0.9.0'?{...s,pages:s.pages.filter(p=>p.slug!=='demos')}:s);
 const view=navigationView(partial,`${base}/en/carddetector/0.9.0/overview/`);
 const demos=sidebarGroups(partial,view).flatMap(g=>g.items).find(p=>p.id==='demos');
 assert.match(demos.href,/development\/demos\/$/);
 assert.match(demos.context,/Development/);
});
test('release and development have the same sidebar ordering and short labels',()=>{
 const release=navigationView(scopes,`${base}/en/carddetector/0.10.0/overview/`);
 const dev=navigationView(scopes,`${base}/en/carddetector/development/overview/`);
 const menu=v=>sidebarGroups(scopes,v).flatMap(g=>g.items.map(p=>[g.id,p.id,p.title]));
 assert.deepEqual(menu(release),menu(dev));
 assert.ok(menu(release).some(([,id])=>id==='releases'));
 assert.ok(menu(release).some(([,id])=>id==='overview'));
 assert.ok(sidebarGroups(scopes,release)[0].items.every(p=>p.href.startsWith(release.scope.root)));
});
test('switching release to development and back preserves the overview or guide',()=>{
 const release=scopes.find(s=>s.language==='en'&&s.version==='0.10.0');
 const dev=scopes.find(s=>s.language==='en'&&s.version==='development');
 for(const slug of ['overview','quickstart','api/components']){
  const view=navigationView(scopes,release.root+slug+'/');
  const next=destination(dev,view);
  assert.equal(next,dev.root+slug+'/');
  assert.equal(destination(release,navigationView(scopes,next)),release.root+slug+'/');
 }
});
test('shared resources retain module/version/language in static URLs',()=>{
 const view=navigationView(scopes,`${base}/es/carddetector/0.9.0/project/releases/`);
 assert.equal(view.scope.version,'0.9.0');assert.equal(view.scope.language,'es');
 const target=scopes.find(s=>s.language==='es'&&s.version==='development');
 assert.equal(destination(target,view),target.root+'project/releases/');
 const resource=sidebarGroups(scopes,view).flatMap(g=>g.items).find(p=>p.id==='media');
 assert.equal(resource.href,view.scope.root+'project/media/');
});
test('old unversioned guide URLs still disclose development scope',()=>{
 const view=navigationView(scopes,`${base}/getting-started/`);
 assert.equal(view.scope.version,'development');assert.equal(view.slug,'quickstart');
});
test('a model keeps all navigation available with explicit detector fallback labels',()=>{
 const view=navigationView(scopes,`${base}/en/tfmodel/0.1.2/overview/`);
 const groups=sidebarGroups(scopes,view);
 const first=groups.flatMap(g=>g.items).find(p=>p.id==='quickstart');
 assert.ok(first.href.includes('/carddetector/0.10.0/quickstart/'));
 assert.match(first.context,/carddetector.*0.10.0/);
 assert.equal(destination(view.scope,{slug:'quickstart',kind:'guide'}),view.scope.root+'overview/');
});
