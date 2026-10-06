"""Independent, journaled releases for carddetector and tfmodel. Never uploads packages."""
import argparse
import hashlib
import io
import zipfile
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import xml.etree.ElementTree as ET
import release as common

ROOT = Path(__file__).resolve().parents[1]
MODULES = {'carddetector': 'core', 'tfmodel': 'sentinel-card-model'}
ARTIFACTS = {'carddetector': ('core', 'card-detector'), 'tfmodel': ('sentinel-card-model', 'card-detector-model')}

def git(*args): return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()
def metadata_path(module): return f'docs/releases/{module}.json'
def tag(module, version): return f'{module}/v{version}'
def pending(module, version): return f'release-pending/{module}/{version}'
def uploading(module, version): return f'release-uploading/{module}/{version}'
def refresh(): git('fetch','origin','main','--tags')
def remote_tags(): return [line.split('refs/tags/',1)[1] for line in git('ls-remote','--refs','--tags','origin').splitlines()]
def remote_ref(name):
    result=git('ls-remote','--refs','origin',f'refs/tags/{name}')
    return result.split()[0] if result else None

def properties():
    return dict(line.strip().split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line and not line.lstrip().startswith('#'))

def identity(module):
    props=properties()
    group=props['GROUP']; artifact=props['POM_ARTIFACT_ID' if module=='carddetector' else 'MODEL_ARTIFACT_ID']
    if not re.fullmatch(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+',group) or artifact not in ARTIFACTS[module]:
        raise ValueError('Unexpected module coordinates')
    return group,artifact

def next_version(module,tags,published,initial=''):
    prefix=module+'/'
    return common.next_version([t[len(prefix):] for t in tags if t.startswith(prefix)],published,initial)

def ensure_available(module,tags):
    legacy=[t for t in tags if re.fullmatch(r'release-(?:pending|uploading)/'+common.SEMVER,t)]
    if legacy: raise ValueError('Unresolved legacy paired release; finalize it using the old workflow before independent releases')
    own=[t for t in tags if t.startswith((f'release-pending/{module}/',f'release-uploading/{module}/'))]
    if own: raise ValueError(f'Unresolved {module} attempt {own}; run finalization, never re-upload')

def validate_record(record,module):
    if module not in MODULES or record.get('module')!=module or record.get('artifact') not in ARTIFACTS[module] or record.get('companions'):
        raise ValueError('Journal must contain exactly the selected module')
    common.version_key(record['version'])
    if not re.fullmatch('[0-9a-f]{40}',record['source']): raise ValueError('Invalid source')
    if set(record['sha256'])!=set(common.SUFFIXES) or any(not re.fullmatch('[0-9a-f]{64}',v) for v in record['sha256'].values()):
        raise ValueError('Incomplete publication hashes')
    if module=='tfmodel':
        common.version_key(record['core_version'])
        if record.get('core_artifact','core') not in ARTIFACTS['carddetector']: raise ValueError('Invalid core artifact pin')
    if record['phase'] not in ('reserved-before-upload','confirmed-public'): raise ValueError('Invalid phase')

def read_record(module,version):
    name=pending(module,version)
    if git('cat-file','-t',f'refs/tags/{name}')!='tag': raise ValueError('Journal must be annotated')
    if remote_ref(name)!=git('rev-parse',f'refs/tags/{name}'): raise ValueError('Remote reservation differs')
    r=json.loads(git('cat-file','-p',f'refs/tags/{name}').split('\n\n',1)[1])
    validate_record(r,module)
    if r['version']!=version or git('rev-parse',f'{name}^{{commit}}')!=r['source']: raise ValueError('Journal source/version mismatch')
    return r

def ensure_core_public(version):
    artifact=properties().get("modelCoreArtifact", "core")
    if artifact not in ARTIFACTS["carddetector"]: raise ValueError("Invalid core artifact pin")
    common.version_key(version)
    group,_=identity('carddetector')
    if version not in common.published_versions(group,artifact): raise ValueError('Pinned model core dependency is not published')
    base=f'{common.artifact_base(group,artifact)}/{version}/{artifact}-{version}'
    xml=ET.fromstring(common.fetch(base+'.pom'))
    for node in xml.iter(): node.tag=node.tag.split('}')[-1]
    if tuple(xml.findtext(k) for k in ('groupId','artifactId','version','packaging'))!=(group,artifact,version,'aar'):
        raise ValueError('Pinned core POM mismatch')
    if not common.fetch(base+'.aar'): raise ValueError('Pinned core AAR is unavailable')

def prepare(module,initial=''):
    refresh(); tags=remote_tags(); ensure_available(module,tags)
    group,artifact=identity(module)
    history=common.published_versions(group,artifact)
    confirmed=json.loads((ROOT/metadata_path(module)).read_text())
    if confirmed:
        validate_record(confirmed,module)
        history=list(set(history+[confirmed['version']]))
    version=next_version(module,tags,history,initial)
    source=git('rev-parse','HEAD');git('merge-base','--is-ancestor',source,'origin/main')
    for name in tags:
        if re.fullmatch(re.escape(module+'/v')+common.SEMVER,name) and git('rev-parse',f'{name}^{{commit}}')==source:
            raise ValueError('Selected module already released from this source')
    if module=='tfmodel': ensure_core_public(properties()['modelCoreVersion'])
    return common.outputs(dict(module=module,version=version,source=source))

def prepare_finalization(module,version,source=''):
    common.version_key(version)
    if source and not re.fullmatch('[0-9a-f]{40}',source): raise ValueError('Invalid source')
    refresh()
    if remote_ref(pending(module,version)):
        r=read_record(module,version)
        if source and source!=r['source']: raise ValueError('Conflicting source')
        git('merge-base','--is-ancestor',r['source'],'origin/main')
        current=json.loads(git('show',f'origin/main:{metadata_path(module)}'))
        if current and common.version_key(current['version'])>=common.version_key(version): raise ValueError('Pending release would overwrite newer documentation')
        return common.outputs(dict(module=module,version=version,source=r['source'],skip='false'))
    stable=tag(module,version)
    current=json.loads(git('show',f'origin/main:{metadata_path(module)}'))
    # Preserve legacy v0.1.0 provenance without inventing new migration tags.
    if current and current['version']==version and current.get('legacy_tag'): stable=current['legacy_tag']
    if not remote_ref(stable) or remote_ref(uploading(module,version)): raise ValueError('No matching pending or completed release')
    tagged=git('rev-parse',f'{stable}^{{commit}}')
    if source and tagged!=source: raise ValueError('Conflicting source')
    validate_record(current,module)
    if current['phase']!='confirmed-public' or common.version_key(current['version'])<common.version_key(version): raise ValueError('Stable tag lacks confirmed docs')
    current_tag=current.get('legacy_tag') or tag(module,current['version'])
    if not remote_ref(current_tag) or git('rev-parse',f'{current_tag}^{{commit}}')!=current['source']: raise ValueError('Confirmed source differs from stable tag')
    git('merge-base','--is-ancestor',tagged,current['source']);git('merge-base','--is-ancestor',current['source'],'origin/main')
    return common.outputs(dict(module=module,version=version,source=tagged,skip='true'))

def model_assets():
    return {'assets/'+p.relative_to(ROOT/'tfmodel/src/main/assets').as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'tfmodel/src/main/assets').rglob('*.tflite')}

def verify_bytes(data,suffix,r):
    common.verify_artifact(data,suffix,r['group'],r['artifact'],r['version'],model_core_version=r.get('core_version'),model_assets=r.get('model_assets'),model_core_artifact=r.get('core_artifact','core'))

def verify_documentation_contents(data):
    # Apply to new local candidates only; historical releases may contain media.
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        unexpected=[entry.filename for entry in archive.infolist()
                    if not entry.is_dir() and not entry.filename.endswith('.md')
                    and entry.filename not in ('LICENSE','META-INF/MANIFEST.MF')]
        if unexpected:raise ValueError(f'Unexpected documentation archive content: {unexpected}')

def local_record(module,version,source):
    common.version_key(version)
    if source!=git('rev-parse','HEAD'): raise ValueError('Checkout differs from source')
    git('diff','--exit-code',source,'--','.')
    group,artifact=identity(module)
    r=dict(module=module,version=version,source=source,group=group,artifact=artifact,sha256={},phase='reserved-before-upload',workflow_run=os.environ.get('GITHUB_RUN_ID','local'))
    if module=='tfmodel':
        r['core_version']=properties()['modelCoreVersion'];common.version_key(r['core_version'])
        r['core_artifact']=properties().get('modelCoreArtifact','core')
        r['model_assets']=model_assets()
        if not r['model_assets']: raise ValueError('No model assets')
    directory=ROOT/'build/verification-repository'/group.replace('.','/')/artifact/version
    for suffix in common.SUFFIXES:
        data=(directory/f'{artifact}-{version}{suffix}').read_bytes();verify_bytes(data,suffix,r)
        if suffix=='-javadoc.jar':verify_documentation_contents(data)
        r['sha256'][suffix]=hashlib.sha256(data).hexdigest()
    validate_record(r,module);return r

def reserve(module,version,source):
    r=local_record(module,version,source)
    if module=='tfmodel':ensure_core_public(r['core_version'])
    base=f'{common.artifact_base(r["group"],r["artifact"])}/{version}/{r["artifact"]}-{version}'
    if any(common.fetch(base+s,missing=True) is not None for s in common.SUFFIXES): raise ValueError('Immutable version already has public artifacts')
    refresh();ensure_available(module,remote_tags());git('merge-base','--is-ancestor',source,'origin/main')
    if remote_ref(tag(module,version)): raise ValueError('Stable tag already exists')
    name=pending(module,version);git('tag','-a',name,source,'-m',json.dumps(r,sort_keys=True));git('push','origin',f'refs/tags/{name}')

def guard(module,version):
    common.version_key(version);r=read_record(module,version)
    if r['source']!=git('rev-parse','HEAD') or (r['group'],r['artifact'])!=identity(module): raise ValueError('Reservation differs from source/module')
    if module=='tfmodel' and (r['core_version']!=properties()['modelCoreVersion'] or r.get('core_artifact','core')!=properties().get('modelCoreArtifact','core')): raise ValueError('Pinned core differs from reservation')
    if remote_ref(tag(module,version)): raise ValueError('Version already finalized')
    marker=uploading(module,version)
    if remote_ref(marker): raise ValueError('Upload already started; finalize without re-uploading')
    git('tag','-a',marker,r['source'],'-m',f'Upload may have started: {pending(module,version)}');git('push','origin',f'refs/tags/{marker}')

def verify_public(r):
    base=f'{common.artifact_base(r["group"],r["artifact"])}/{r["version"]}/{r["artifact"]}-{r["version"]}'
    for suffix in common.SUFFIXES:
        data=common.fetch(base+suffix);verify_bytes(data,suffix,r)
        if hashlib.sha256(data).hexdigest()!=r['sha256'][suffix]: raise ValueError('Public artifact hash mismatch')
        if b'BEGIN PGP SIGNATURE' not in common.fetch(base+suffix+'.asc'): raise ValueError('Missing signature')

def confirm(module,version,timeout=2400):
    common.version_key(version);r=read_record(module,version)
    if r['source']!=git('rev-parse','HEAD'): raise ValueError('Confirmation requires immutable source checkout')
    deadline=time.monotonic()+timeout
    while True:
        try:verify_public(r);break
        except urllib.error.HTTPError as e:
            if e.code not in (404,429) and e.code<500: raise
            last=e
        except (urllib.error.URLError,TimeoutError) as e:last=e
        if time.monotonic()>=deadline: raise TimeoutError('Public artifacts not confirmed; preserve journal and retry finalization') from last
        time.sleep(min(20,max(0,deadline-time.monotonic())))
    r['phase']='confirmed-public'
    path=ROOT/f'build/confirmed-{module}.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(r,indent=2,sort_keys=True)+'\n')
    return r

def documentation(verify=False):
    records={module:json.loads((ROOT/metadata_path(module)).read_text()) for module in MODULES}
    sections=[]
    for module,r in records.items():
        if r is None:sections.append(f'## {MODULES[module]}\n\nNo confirmed release.');continue
        validate_record(r,module)
        if r['phase']!='confirmed-public':raise ValueError('Installation can only advertise confirmed releases')
        configured=identity(module)
        notice=''
        if (r['group'],r['artifact'])!=configured:
            notice=f'\nConfigured next publication: `{configured[0]}:{configured[1]}` — not yet confirmed here. The dependency below remains the last confirmed publication.\n'
        gav=f'{r["group"]}:{r["artifact"]}:{r["version"]}'
        dep=f"\nExports `{r['group']}:{r.get('core_artifact','core')}:{r['core_version']}`; the model version is independent.\n" if module=='tfmodel' else ''
        sections.append(f'''## {r['artifact']}

Confirmed version: **{r['version']}**. Source: [{r['source']}](https://github.com/lambdawalker/android.apexfission.carddetector/commit/{r['source']}).
{notice}{dep}
### Gradle Kotlin DSL

```kotlin
implementation("{gav}")
```

### Gradle Groovy DSL

```groovy
implementation '{gav}'
```

### Version catalog

```toml
[libraries]
{module} = {{ module = "{r['group']}:{r['artifact']}", version = "{r['version']}" }}
```

### Maven

```xml
<dependency>
  <groupId>{r['group']}</groupId>
  <artifactId>{r['artifact']}</artifactId>
  <version>{r['version']}</version>
  <type>aar</type>
</dependency>
```''')
    template=(ROOT/'docs/templates/MODULE_IMPORT.md.template').read_text()
    expected=template.replace('{{INSTALLATION}}','\n\n'.join(sections))
    if '{{' in expected:raise ValueError('Unknown template placeholder')
    path=ROOT/'IMPORT.md'
    if verify:
        if path.read_text()!=expected:raise ValueError('IMPORT.md drift; run ./gradlew generateImportDocs')
    else:path.write_text(expected)

def finalize(module,version,source):
    common.version_key(version)
    if not re.fullmatch('[0-9a-f]{40}',source):raise ValueError('Invalid source')
    r=json.loads((ROOT/f'build/confirmed-{module}.json').read_text());validate_record(r,module)
    if (r['version'],r['source'],r['phase'])!=(version,source,'confirmed-public'):raise ValueError('Confirmation mismatch')
    journal=read_record(module,version)
    if {**journal,'phase':'confirmed-public'}!=r:raise ValueError('Confirmation differs from reservation')
    refresh();git('merge-base','--is-ancestor',source,'origin/main')
    # Another module's finalized metadata is deliberately excluded; read it from latest main below.
    git('diff','--exit-code',source,'origin/main','--','scripts','gradle.properties','build.gradle.kts',f'{module}/build.gradle.kts','settings.gradle.kts','gradle/libs.versions.toml','docs/templates/MODULE_IMPORT.md.template','.github/workflows/publish-card-detection.yml','.github/workflows/finalize-carddetector.yml')
    git('switch','-C',f'finalize-{module}','origin/main')
    current=json.loads((ROOT/metadata_path(module)).read_text())
    if current and common.version_key(current['version'])>=common.version_key(version):raise ValueError('Refusing to replace same/newer module metadata')
    (ROOT/metadata_path(module)).write_text(json.dumps(r,indent=2,sort_keys=True)+'\n');documentation()
    git('config','user.name','github-actions[bot]');git('config','user.email','41898282+github-actions[bot]@users.noreply.github.com')
    git('add',metadata_path(module),'IMPORT.md');git('commit','-m',f'docs: confirm {module} {version}')
    stable=tag(module,version)
    if remote_ref(stable):raise ValueError('Stable tag already exists; reconcile before finalization')
    git('tag','-a',stable,source,'-m',f'Maven Central {module} {version}')
    updates=['HEAD:refs/heads/main',f'refs/tags/{stable}',f':refs/tags/{pending(module,version)}']
    if remote_ref(uploading(module,version)):
        if git('rev-parse',f'{uploading(module,version)}^{{commit}}')!=source:raise ValueError('Upload marker source mismatch')
        updates.append(f':refs/tags/{uploading(module,version)}')
    git('push','--atomic','origin',*updates)

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('command',choices=['prepare','prepare-finalization','check-local','reserve','guard','confirm','finalize','generate','verify'])
    p.add_argument('--module',choices=MODULES);p.add_argument('--version');p.add_argument('--source',default='');p.add_argument('--initial',default='');p.add_argument('--timeout',type=int,default=2400);a=p.parse_args()
    if a.command in ('generate','verify'):return documentation(a.command=='verify')
    if not a.module:p.error('--module is required')
    if a.command=='prepare':return prepare(a.module,a.initial)
    if not a.version:p.error('--version is required')
    if a.command=='prepare-finalization':return prepare_finalization(a.module,a.version,a.source)
    if a.command in ('check-local','reserve','finalize') and not a.source:p.error('--source is required')
    if a.command=='check-local':print(json.dumps(local_record(a.module,a.version,a.source),indent=2))
    elif a.command=='reserve':reserve(a.module,a.version,a.source)
    elif a.command=='guard':guard(a.module,a.version)
    elif a.command=='confirm':confirm(a.module,a.version,a.timeout)
    elif a.command=='finalize':finalize(a.module,a.version,a.source)
if __name__=='__main__':main()
