import {readFile,writeFile,mkdir,copyFile,readdir} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {modelPinNotice} from './scripts/model-pin.mjs';
import {showcase} from './scripts/showcase.mjs';
import {modelReference,modelOverview,modelPrerequisite,insertGuideNotice} from './scripts/projection.mjs';
const stage=process.env.ANDROID_REPO_DOCS_STAGE;
if(!stage)throw Error('Run through android-repo-docs build; missing staged content');
const read=async name=>JSON.parse(await readFile(resolve(stage,name+'.json'),'utf8'));
let pages=await read('pages');const scopes=await read('scopes'),site=await read('site');
const base=(site.prefix||'').replace(/\/$/,'');
const builder=scopes.find(scope=>scope.development)?.source;
const repository='https://github.com/lambdawalker/android.apexfission.carddetector';
const put=async(path,data)=>{const target=resolve(stage,'public',path.replace(/^\//,''));await mkdir(dirname(target),{recursive:true});await writeFile(target,data);};
const local=href=>href.slice(base.length).replace(/^\//,'');
const rawPath=(scope,slug)=>local(scope.root)+'raw/'+(slug||'index')+'.md';
const replacements=[];
for(let i=0;i<scopes.length;i++){
 const scope=scopes[i];
 if(scope.module==='tfmodel'){
  const allowed=slug=>['','index','installation','model-contract','api','api/detection','overview'].includes(slug)||slug.startsWith('project/');
  for(const page of scope.pages.filter(page=>!allowed(page.slug))){
   const immutable=`${repository}/blob/${scope.documentation_ref}/docs/agents/${page.slug}.md`;
   replacements.push([page.raw,immutable],[page.href,immutable],[scope.root+'agents/'+page.slug+'.md',immutable]);
  }
  scope.pages=scope.pages.filter(page=>allowed(page.slug));
  pages=pages.filter(page=>page.scope_index!==i||allowed(page.guide));
  for(const page of pages.filter(page=>page.scope_index===i)){
   if(['','api'].includes(page.guide)){page.fallback=false;page.current_resource=true;page.source=builder;page.documentation_ref=builder;}
   if(page.guide===''){page.title=scope.language==='es'?'Modelo de detección':'Detection model';page.body=modelOverview(scope);}
   if(page.guide==='api'){page.title=scope.language==='es'?'API del modelo':'Model API';page.body=`[${page.title}](${scope.root}api/detection/)\n`;}
   if(page.guide==='api/detection'){page.title=scope.language==='es'?'API del modelo':'Model API';page.body=modelReference(page.body);}
   if(page.guide==='installation'&&!scope.development){if(!scope.release)throw Error('Missing immutable model release metadata');page.body+='\n'+modelPinNotice(scope.release,scope.language);}
   const metadata=scope.pages.find(item=>item.slug===page.guide);if(metadata)metadata.title=page.title;
  }
 }
 if(scope.module==='carddetector'){
  const model=scopes.filter(s=>s.module==='tfmodel'&&s.language===scope.language&&!s.development).sort((a,b)=>a.version.localeCompare(b.version,undefined,{numeric:true})).at(-1);
  for(const page of pages.filter(page=>page.scope_index===i&&page.guide==='quickstart')){
   page.body=modelPrerequisite(scope,model?model.root+'installation/':`${base}/${scope.language}/`)+page.body;
   page.body=page.body.replaceAll(scope.root+'installation/',model?model.root+'installation/':`${base}/${scope.language}/`);
  }
 }
 const currentNotice=scope.language==='es'?`> Recurso actual del proyecto: vistas ilustrativas mantenidas con el sitio actual (\`${builder}\`), no evidencia de esta publicación.\n\n`:`> Current project resource: illustrative previews maintained with the current site (\`${builder}\`), not release evidence.\n\n`;
 const overview={path:local(scope.root)+'overview',title:scope.language==='es'?'Detecta. Sigue. Captura.':'Detect. Track. Capture.',body:currentNotice+showcase(scope,base).replace(/^# .+\n/,''),scope_index:i,guide:'overview',fallback:false,title_anchor:'overview',current_resource:true,source:builder,documentation_ref:builder};
 pages.push(overview);scope.pages.push({slug:'overview',title:overview.title,href:scope.root+'overview/',raw:scope.root+'raw/overview.md'});
}
replacements.sort((a,b)=>b[0].length-a[0].length);
const rewrite=text=>replacements.reduce((result,[from,to])=>from?result.replaceAll(from,to):result,text);
for(const page of pages){
 const scope=scopes[page.scope_index];
 const currentCore=scopes.find(item=>item.module==='carddetector'&&item.language===scope.language&&item.development);
 if(page.current_resource&&page.guide.startsWith('project/'))page.body=page.body.replaceAll(scope.root,currentCore.root);
 page.body=rewrite(page.body);
 const provenance=`> Module: ${scope.module} · Version: ${scope.version} · Source: \`${page.current_resource?page.source:scope.source}\` · Documentation: \`${page.current_resource?page.documentation_ref:scope.documentation_ref}\`${scope.development?' · Development (unreleased)':''}\n\n`;
 const fallback=page.fallback?'> Translation unavailable: showing English from the same documentation revision.\n\n':'';
 let raw;
 try { raw=await readFile(resolve(stage,'public',rawPath(scope,page.guide)),'utf8'); } catch { raw=`# ${page.title}\n\n${provenance}${fallback}${page.body}`; }
 if(scope.module==='tfmodel') {
  if(['','api'].includes(page.guide))raw=`# ${page.title}\n\n${provenance}${fallback}${page.body}`;
  else if(page.guide==='api/detection')raw=`# ${page.title}\n\n${provenance}${fallback}${modelReference(raw)}`;
  else if(page.guide==='installation'&&!scope.development)raw+='\n'+modelPinNotice(scope.release,scope.language);
 }
 if(scope.module==='carddetector'&&page.guide==='quickstart') {
  const target=scopes.filter(s=>s.module==='tfmodel'&&s.language===scope.language&&!s.development).sort((a,b)=>a.version.localeCompare(b.version,undefined,{numeric:true})).at(-1);
  raw=insertGuideNotice(raw,modelPrerequisite(scope,target?target.root+'installation/':`${base}/${scope.language}/`));
  raw=raw.replaceAll(scope.root+'raw/installation.md',target?target.root+'raw/installation.md':`${base}/${scope.language}/`);
 }
 if(page.current_resource&&page.guide.startsWith('project/'))raw=raw.replaceAll(scope.root,currentCore.root);
 raw=rewrite(raw);await put(rawPath(scope,page.guide),raw);
 if(page.guide==='installation')await put(local(scope.root)+'IMPORT.md',raw);
 else if(!page.guide.startsWith('project/')&&page.guide!=='overview')await put(local(scope.root)+'agents/'+(page.guide||'index')+'.md',raw);
}
async function walk(dir){let files=[];for(const item of await readdir(dir,{withFileTypes:true})){const path=resolve(dir,item.name);files.push(...item.isDirectory()?await walk(path):[path]);}return files;}
// Keep old static redirects/raw aliases from advertising removed model-only detector guides.
for(const file of await walk(resolve(stage,'public'))){if(!/\.(?:html|md)$/.test(file))continue;const text=await readFile(file,'utf8');const changed=rewrite(text);if(changed!==text)await writeFile(file,changed);}
for(const [from,to] of replacements.filter(([from])=>from.includes('/raw/')||from.includes('/agents/')))await put(local(from),`# Detector documentation\n\n[Read this guide at its immutable documentation revision](${to}).\n`);
// Legacy version-only paths prefer the detector when versions overlap; scoped model paths remain independent.
const legacyScopes=new Map();
for(const scope of scopes){const key=scope.version+'/'+scope.language;const previous=legacyScopes.get(key);if(!previous||scope.module==='carddetector')legacyScopes.set(key,scope);}
for(const [prefix,scope] of legacyScopes){for(const page of scope.pages){
 if(page.slug==='overview'||page.slug.startsWith('project/'))continue;
 const raw=await readFile(resolve(stage,'public',rawPath(scope,page.slug)),'utf8');
 await put(prefix+'/'+(page.slug==='installation'?'IMPORT.md':'agents/'+(page.slug||'index')+'.md'),raw);
}}
// Root agent endpoints always identify current detector development, never the model or a release.
const core=scopes.find(scope=>scope.module==='carddetector'&&scope.language==='en'&&scope.development);
if(!core)throw Error('Current detector scope missing');
for(const page of core.pages){if(page.slug.startsWith('project/')||page.slug==='overview')continue;const raw=await readFile(resolve(stage,'public',rawPath(core,page.slug)),'utf8');await put(page.slug==='installation'?'IMPORT.md':'agents/'+(page.slug||'index')+'.md',raw);}
// Former unscoped human URLs remain current-detector entry aliases.
const legacySlugs={'':'agents','quickstart':'getting-started','api':'reference','recipes':'task-recipes'};
for(const page of core.pages){
 const old=page.slug.startsWith('project/')?page.slug.slice('project/'.length):(legacySlugs[page.slug]??page.slug);
 if(!old||page.slug==='overview')continue;
 await put(old+'/index.html',`<!doctype html><html lang="en"><meta charset="utf-8"><meta http-equiv="refresh" content="0;url=${page.href}"><script>location.replace(${JSON.stringify(page.href)}+location.search+location.hash)</script><a href="${page.href}">Current detector documentation</a></html>`);
}
await put('llms.txt',`# Card Detector\n\nCurrent detector development documentation. Model releases and their detector pins are selected independently.\n\n- [Agent entry](${base}/agents/index.md)\n- [Installation](${base}/IMPORT.md)\n- [API](${base}/agents/api.md)\n- [Quickstart](${base}/agents/quickstart.md)\n- [Model versions](${base}/en/)\n`);
await mkdir(resolve(stage,'public/screenshots'),{recursive:true});
for(const name of await readdir('docs/screenshots'))if(/\.(png|json)$/.test(name))await copyFile(resolve('docs/screenshots',name),resolve(stage,'public/screenshots',name));
const video=await readFile('docs/demo.mp4');if(video.length<1000000||video.subarray(0,100).toString().includes('git-lfs.github.com'))throw Error('Recording is not hydrated');
await put('media/card-detection.mp4',video);
await Promise.all([writeFile(resolve(stage,'pages.json'),JSON.stringify(pages,null,2)+'\n'),writeFile(resolve(stage,'scopes.json'),JSON.stringify(scopes,null,2)+'\n'),put('versions.json',JSON.stringify(scopes,null,2)+'\n')]);
console.log(`Applied detector/model projections to ${scopes.length} immutable/development scopes.`);
