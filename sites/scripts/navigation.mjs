import {base} from './markdown.mjs';

const definitions=[
 {id:'start',titles:['Start here','Primeros pasos'],items:[
  ['overview','overview','Overview','Vista general'],['installation','installation','Install','Instalar'],
  ['quickstart','quickstart','First detection','Primera detección'],['model-contract','model-contract','Model contract','Contrato del modelo']]},
 {id:'integration',titles:['Integration','Integración'],items:[
  ['configuration','configuration','Configure detection','Configurar detección'],['concepts','concepts','Threads & ownership','Hilos y propiedad'],
  ['recipes','recipes','Recipes','Recetas'],['demos','demos','Runnable demos','Demos ejecutables'],['integration-map','','Integration map','Mapa de integración']]},
 {id:'reference',titles:['API & troubleshooting','API y diagnóstico'],items:[
  ['api','api','API index','Índice de API'],['components','api/components','Components','Componentes'],
  ['detection','api/detection','Detection','Detección'],['overlays','api/overlays','Overlays','Capas visuales'],
  ['adapters','api/adapters','Adapters','Adaptadores'],['utilities','api/utilities','Utilities','Utilidades'],
  ['limitations','limitations','Limitations','Limitaciones'],['troubleshooting','troubleshooting','Troubleshooting','Solución de problemas'],
  ['migration','migration','Migration','Migración']]},
];
const projects=[
 ['development','Build & maintain','Compilar y mantener'],['releases','Release runbook','Publicar versiones'],
 ['coverage','Feature coverage','Cobertura de funciones'],['media','Screenshots & media','Capturas y medios'],
];
const legacy=new Map([
 ['agents',''],['installation','installation'],['getting-started','quickstart'],['reference','api'],['task-recipes','recipes'],
 ...definitions.flatMap(g=>g.items).filter(([,slug])=>slug!=='overview').map(([,slug])=>[slug,slug]),
]);
const cmp=(a,b)=>{
 const aa=a.version.split('.').map(Number),bb=b.version.split('.').map(Number);
 for(let i=0;i<3;i++)if(aa[i]!==bb[i])return aa[i]-bb[i];
 return 0;
};
export function latestScope(scopes,language='en',module='carddetector') {
 const candidates=scopes.filter(s=>s.language===language&&s.module===module);
 return candidates.filter(s=>s.version!=='development').sort(cmp).at(-1)
  ?? candidates.find(s=>s.version==='development');
}
export function versionScopes(scopes,language,module) {
 const matches=scopes.filter(s=>s.language===language&&s.module===module);
 return [...matches.filter(s=>s.version!=='development').sort((a,b)=>cmp(b,a)),...matches.filter(s=>s.version==='development')];
}
const hasPage=(scope,slug)=>scope.pages?.some(p=>p.slug===slug);
const pageUrl=(scope,slug)=>scope.root+(slug?slug+'/':'');
function projectUrl(scope,slug) {
 return pageUrl(scope,'project/'+slug);
}
export function navigationView(scopes,path,params=new URLSearchParams()) {
 const current=scopes.find(s=>path.startsWith(s.root));
 if(current){
  const slug=path.slice(current.root.length).replace(/\/$/,'');
  return {scope:current,kind:slug.startsWith('project/')?'project':'guide',slug:slug.replace(/^project\//,'')};
 }
 const relative=path.slice(base.length).replace(/^\/+|\/+$/g,'');
 const catalog=/^(en|es)$/.test(relative);
 const language=catalog?relative:(params.get('lang')==='es'?'es':'en');
 const module=params.get('module')==='tfmodel'?'tfmodel':'carddetector';
 if(relative&&legacy.has(relative)&&!projects.some(([slug])=>slug===relative)) {
  return {scope:scopes.find(s=>s.language===language&&s.module==='carddetector'&&s.version==='development')??latestScope(scopes,language),kind:'guide',slug:legacy.get(relative)};
 }
 const scope=scopes.find(s=>s.language===language&&s.module===module&&s.version===params.get('version'))??latestScope(scopes,language,module);
 return {scope,kind:catalog?'catalog':projects.some(([slug])=>slug===relative)?'project':'guide',slug:catalog?'':projects.some(([slug])=>slug===relative)?relative:'overview'};
}
export function destination(scope,view) {
 if(view.kind==='project')return projectUrl(scope,view.slug);
 if(view.kind==='catalog')return pageUrl(scope,'overview');
 return pageUrl(scope,hasPage(scope,view.slug)?view.slug:'overview');
}
export function sidebarGroups(scopes,view) {
 const es=view.scope.language==='es',idx=es?1:0;
 const groups=definitions.map(group=>({id:group.id,title:group.titles[idx],items:group.items.map(([id,slug,en,spanish])=>{
  let scope=view.scope;
  if(!hasPage(scope,slug)) {
   const detector=scope.module==='tfmodel'?latestScope(scopes,scope.language,'carddetector'):undefined;
   scope=detector&&hasPage(detector,slug)?detector:scopes.find(s=>s.module===scope.module&&s.language===scope.language&&s.version==='development'&&hasPage(s,slug));
  }
  const target=scope??view.scope;
  return {id,title:es?spanish:en,href:pageUrl(target,hasPage(target,slug)?slug:'overview'),
   context:target!==view.scope?`${target.module} · ${target.version==='development'?(es?'Desarrollo':'Development'):target.version}`:undefined,
   current:view.kind==='guide'&&view.slug===slug&&target===view.scope};
 })}));
 groups.push({id:'project',title:es?'Proyecto':'Project',items:[
  ...projects.map(([slug,en,spanish])=>({id:slug,title:es?spanish:en,href:projectUrl(view.scope,slug),current:view.kind==='project'&&view.slug===slug})),
  {id:'markdown',title:es?'Markdown para agentes':'Markdown for agents',href:view.scope.root+'raw/index.md'},
 ]});
 return groups;
}
/** Catalog entries open the same visual overview, with versioned guides available beside it. */
export function catalogLinks(scopes,language) {
 return scopes.filter(scope=>scope.language===language).map(scope=>({
  title:`${scope.module} · ${scope.version==='development'?(language==='es'?'Desarrollo':'Development'):scope.version}`,
  href:pageUrl(scope,hasPage(scope,'overview')?'overview':''),
 }));
}
