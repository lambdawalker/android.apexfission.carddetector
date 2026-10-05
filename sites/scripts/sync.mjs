import {readFile,writeFile,mkdir,rm,readdir,copyFile} from 'node:fs/promises';
import {resolve,dirname,relative} from 'node:path';
import {execFileSync} from 'node:child_process';
import {rewriteMarkdown,humanPages,base,repository} from './markdown.mjs';
const root=resolve('..');
process.env.DOCS_REF ||= execFileSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'}).trim();
if(!/^[a-f0-9]{40}$/.test(process.env.DOCS_REF))throw new Error('DOCS_REF must be an immutable full commit SHA');
for(const script of ['module_release','docs','media'])execFileSync('python3',[`scripts/${script}.py`,'verify'],{cwd:root,stdio:'inherit'});
const read=p=>readFile(resolve(root,p),'utf8');
async function put(p,s){await mkdir(dirname(p),{recursive:true});await writeFile(p,s);}
async function walk(dir){let out=[];for(const e of await readdir(dir,{withFileTypes:true})){const p=resolve(dir,e.name);out.push(...e.isDirectory()?await walk(p):[p]);}return out;}
await rm('public',{recursive:true,force:true});
await rm('src/content/docs',{recursive:true,force:true});
await mkdir('public/agents',{recursive:true});
await mkdir('public/media',{recursive:true});
await mkdir('public/screenshots',{recursive:true});
await put('public/favicon.svg', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="12" fill="#006973"/><rect x="12" y="20" width="40" height="26" rx="5" fill="none" stroke="white" stroke-width="4"/><path d="M18 28h8m-8 8h20" stroke="white" stroke-width="3"/></svg>');
await copyFile(resolve(root,'docs/Screen_recording_20260917_121435.mp4'),'public/media/card-detection.mp4');
const manifest=JSON.parse(await read('docs/screenshots/manifest.json'));
for(const s of manifest.scenarios)await copyFile(resolve(root,'docs/screenshots',s.file),resolve('public/screenshots',s.file));
await copyFile(resolve(root,'docs/screenshots/manifest.json'),'public/screenshots/manifest.json');
const core=JSON.parse(await read('docs/releases/carddetector.json'));
const model=JSON.parse(await read('docs/releases/tfmodel.json'));
const release={version:`core ${core.version} / model ${model.version}`};
await put('src/build.json',JSON.stringify({ref:process.env.DOCS_REF,version:release.version}));
const stamp=`> Main development documentation. Build source: [${process.env.DOCS_REF.slice(0,12)}](${repository}/commit/${process.env.DOCS_REF}). Confirmed dependency facts: [IMPORT.md](${base}/IMPORT.md).\n\n`;
const player=`<figure class="recording"><video controls playsinline preload="metadata" poster="${base}/screenshots/tracking.png" aria-label="Historical screen recording of card detection guidance" aria-describedby="recording-description"><source src="${base}/media/card-detection.mp4" type="video/mp4" /><p><a href="${base}/media/card-detection.mp4">Download the recording</a></p></video><figcaption id="recording-description">31-second historical demonstration. The guide follows a sample card; no narrated audio. Capture source and device unknown. See the visual description below.</figcaption></figure>`;
async function human(source,slug){
 const md=await read(source);const title=md.match(/^# (.+)$/m)?.[1];if(!title)throw new Error(`No title: ${source}`);
 const body=rewriteMarkdown(md.replace(/^# .+\n/m,''),source,'human').replace('<!-- recording-player -->',player);
 await put(`src/content/docs/${slug}.md`,'---\ntitle: '+JSON.stringify(title)+'\n---\n\n'+body);
}
for(const file of await walk(resolve(root,'docs/agents'))){if(!file.endsWith('.md'))continue;
 const source=relative(root,file),guide=relative(resolve(root,'docs/agents'),file);
 await put(resolve('public/agents',guide),stamp+rewriteMarkdown(await read(source),source));
 await human(source,humanPages[guide]||guide.replace(/\.md$/,''));
}
await put('public/IMPORT.md',rewriteMarkdown(await read('IMPORT.md'),'IMPORT.md'));
for(const [source,slug] of [['docs/overview.md','index'],['IMPORT.md','installation'],['docs/maintenance.md','development'],['docs/coverage.md','coverage'],['docs/releases.md','releases'],['docs/screenshots/README.md','media']])await human(source,slug);
await put('public/llms.txt',`# Card Detector\n\nMain documentation at ${process.env.DOCS_REF}.\n\n- [Agent entry](${base}/agents/index.md)\n- [Installation](${base}/IMPORT.md)\n- [API](${base}/agents/api.md)\n- [Quickstart](${base}/agents/quickstart.md)\n- [Ownership](${base}/agents/concepts.md)\n- [Limitations](${base}/agents/limitations.md)\n`);
console.log('Synchronized human guides, raw Markdown, exact examples and historical media.');
