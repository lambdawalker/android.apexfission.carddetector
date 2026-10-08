import {execFileSync} from 'node:child_process';
import {writeFile,mkdir} from 'node:fs/promises';
import {dirname} from 'node:path';
import {readRevision,readTranslationTree,readWorkingDocs,translatedGuide,rewriteVersioned,modelReference,modelPrerequisite,stableAnchors,guideSlug,scopePath} from './versioned.mjs';
import {base,repository} from './markdown.mjs';

export async function buildVersioned(root) {
 const catalog=JSON.parse(execFileSync('python3',['scripts/documentation_history.py','export'],{cwd:root,encoding:'utf8',maxBuffer:16*1024*1024}));
 const put=async(path,text)=>{await mkdir(dirname(path),{recursive:true});await writeFile(path,text);};
 const snapshots=new Map(),scopes=[];
 const working=readWorkingDocs(root);
 const entries=[...catalog,...['carddetector','tfmodel'].map(module=>({module,version:'development',source:process.env.DOCS_REF,documentation_ref:process.env.DOCS_REF}))];
  for(const entry of entries) {
  const dev=entry.version==='development',ref=entry.documentation_ref;
  if(!dev&&!snapshots.has(ref))snapshots.set(ref,readRevision(root,ref));
  const files=new Map(dev?working:snapshots.get(ref));
  if(entry.translation_tree)for(const [path,text] of readTranslationTree(root,entry.translation_tree))files.set(path,text);
  const available=[...files.keys()].filter(p=>p.startsWith('docs/agents/')&&p.endsWith('.md')).map(p=>p.slice(12));
  if(!available.includes('index.md'))throw new Error(`No library guides at ${ref}; explicitly record a reviewed documentation correction`);
  for(const language of ['en','es']) {
   const es=language==='es',model=entry.module==='tfmodel';
   const pageNames=model?['index.md','model-contract.md','api/detection.md']:available;
   const context={language,module:entry.module,version:entry.version,ref,source:entry.source,pages:new Set(pageNames)};
   const prefix=scopePath(context),scope={...context,source:entry.source,root:prefix+'/',pages:[]};
   const sourceLink=`[${entry.source.slice(0,12)}](${repository}/commit/${entry.source})`;
   const docsLink=`[${ref.slice(0,12)}](${repository}/commit/${ref})`;
   const translationNote=entry.translation_tree?` · ${es?'Traducción':'Translation'}: \`${entry.translation_tree.slice(0,12)}\``:'';
   const notice=es
    ? `> **${dev?'Documentación de desarrollo; puede incluir API aún no publicada':`Documentación de ${entry.module} ${entry.version}`}** · Código: ${sourceLink} · Documentación: ${docsLink}${translationNote}. ${!dev?'Las menciones a main en las guías archivadas se refieren a la revisión indicada.':''}\n\n`
    : `> **${dev?'Development documentation; may include unreleased API':`${entry.module} ${entry.version} documentation`}** · Code: ${sourceLink} · Documentation: ${docsLink}${translationNote}. ${!dev?'References to main in archived guides mean the recorded revision.':''}\n\n`;
   async function page(guide,markdown,fallback=false) {
    const slug=guideSlug(guide),title=markdown.match(/^# (.+)$/m)?.[1];
    if(!title)throw new Error(`No title: ${guide}`);
    scope.pages.push({slug,title});
    const warning=fallback?(es?'> **Traducción pendiente:** esta página muestra el original en inglés de esta misma revisión.\n\n':'> Translation pending.\n\n'):'';
    const sourcePath='docs/agents/'+guide;
    if(!model&&guide==='quickstart.md')markdown=markdown.replace(/^(# .+\n)/,'$1\n'+modelPrerequisite(language));
    const raw=notice+warning+rewriteVersioned(markdown,sourcePath,context,'raw');
    await put(`public/${language}/${entry.module}/${entry.version}/raw/${guide}`,raw);
    const body=rewriteVersioned(markdown.replace(/^# .+\n/m,''),sourcePath,context);
    await put(`src/content/docs/${language}/${entry.module}/${entry.version}/${slug||'index'}.md`,
     `---\nslug: ${JSON.stringify(`${language}/${entry.module}/${entry.version}${slug?"/"+slug:""}`)}\ntitle: ${JSON.stringify(title)}\neditUrl: false\nprev: false\nnext: false\n---\n\n${notice}${warning}${body}\n\n[${es?'Leer Markdown':'Read Markdown'}](${prefix}/raw/${guide})\n`);
   }
   for(const guide of pageNames) {
    if(model&&guide==='index.md') {
     await page(guide,es?`# Modelo de detección\n\nEste módulo empaqueta el modelo TensorFlow Lite y sus extensiones de catálogo. Su versión es independiente de la del detector.\n\n1. Consulta la [instalación](../../IMPORT.md) para ver el detector fijado que exporta esta publicación.\n2. Revisa el [contrato del modelo](model-contract.md).\n3. Usa la [API del catálogo](api/detection.md).\n\nLas guías del detector se seleccionan por separado; no se debe asumir que su versión más reciente coincide con la dependencia fijada del modelo.\n`
      :`# Detection model\n\nThis module packages the TensorFlow Lite model and catalog extensions. Its version is independent of the detector.\n\n1. Check [installation](../../IMPORT.md) for the pinned detector exported by this publication.\n2. Read the [model contract](model-contract.md).\n3. Use the [catalog API](api/detection.md).\n\nSelect detector guides separately; do not assume its latest version matches the model's pinned dependency.\n`);
     continue;
    }
    let {markdown,fallback}=translatedGuide(files,guide,language);
    if(model&&guide==='api/detection.md')markdown=modelReference(markdown,language);
    else if(es&&!fallback)markdown=stableAnchors(files.get('docs/agents/'+guide),markdown);
    await page(guide,markdown,fallback);
   }
   let install=entry.installation?.[language];
   if(dev) {
    const latest=catalog.filter(e=>e.module===entry.module).at(-1);
    install=es?`# Instalación\n\nEl código de desarrollo puede contener API aún no publicada.\n\n${latest?`[Instalar la última versión confirmada: ${latest.version}](${base}/es/${entry.module}/${latest.version}/installation/)`:'No hay ninguna versión confirmada.'}\n`
      :`# Installation\n\nDevelopment source may contain unreleased API.\n\n${latest?`[Install latest confirmed version: ${latest.version}](${base}/en/${entry.module}/${latest.version}/installation/)`:'No confirmed release.'}\n`;
   }
   await page('installation.md',install);
   scopes.push(scope);
  }
 }
 for(const language of ['en','es']) {
  const es=language==='es';let body=`# ${es?'Documentación de la biblioteca':'Library documentation'}\n\n${es?'Selecciona un módulo y una versión. Cada módulo se publica de forma independiente.':'Choose a module and version. Each module releases independently.'}\n\n`;
  for(const module of ['carddetector','tfmodel']) {
   body+=`## ${module}\n\n`;
   for(const e of catalog.filter(e=>e.module===module).reverse())body+=`- [${e.version}](${base}/${language}/${module}/${e.version}/) — ${Object.keys(e.destinations).join(', ')}\n`;
   body+=`- [${es?'Desarrollo (sin publicar)':'Development (unreleased)'}](${base}/${language}/${module}/development/)\n\n`;
  }
  body+=es?'El archivo comienza con las publicaciones confirmadas disponibles al incorporar este sistema; no representa un historial completo de versiones anteriores.\n':'This archive starts with confirmed publications available when this system was introduced; it is not a complete record of older releases.\n';
  const title=body.match(/^# (.+)/)[1];
  await put(`src/content/docs/${language}/index.md`,`---\ntitle: ${JSON.stringify(title)}\nprev: false\nnext: false\n---\n\n`+body.replace(/^# .+\n/,''));
 }
 await put('src/versions.json',JSON.stringify(scopes,null,2)+'\n');
 console.log(`Rendered ${catalog.length} archived releases and bilingual development documentation.`);
}
