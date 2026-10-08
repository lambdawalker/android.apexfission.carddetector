"""Independent, journaled releases for carddetector and tfmodel. Never uploads packages."""
import argparse
import base64
import urllib.parse
import urllib.request
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
import release_identity
import import_docs
import jitpack_release
from publishing_config import load_config, validate_url, ID

ROOT = Path(__file__).resolve().parents[1]
MODULES = {'carddetector': 'core', 'tfmodel': 'sentinel-card-model'}
ARTIFACTS = {'carddetector': ('core', 'card-detector'), 'tfmodel': ('sentinel-card-model', 'card-detector-model')}

def git(*args): return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()
def repository():
    value=os.environ.get('RELEASE_REPOSITORY','maven-central')
    if value not in load_config(): raise ValueError('Unknown release repository')
    return value

def repository_url():
    # Keep the old Central default for unsigned local builds and historical recovery.
    if repository()=='jitpack': return jitpack_release.REPOSITORY
    default=common.CENTRAL if repository()=='maven-central' else ''
    return validate_url(os.environ.get('MAVEN_REPOSITORY_URL','') or default)

def scope(): return '' if repository()=='maven-central' else repository()+'/'
def metadata_path(module): return f'docs/releases/{scope()}{module}.json'
def tag(module, version): return release_identity.canonical(module,version)
def pending(module, version): return f'release-pending/{scope()}{module}/{version}'
def uploading(module, version): return f'release-uploading/{scope()}{module}/{version}'
def artifact_base(group,artifact):
    return f'{repository_url()}/{group.replace(".","/")}/{artifact}'

class NoRepositoryRedirects(urllib.request.HTTPRedirectHandler):
    # Never forward repository credentials to a redirect destination.
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError('Repository redirected; configure its canonical HTTPS URL')

def fetch(url,missing=False):
    if repository()=='maven-central': return common.fetch(url,missing=missing)
    if not url.startswith(repository_url()+'/'): raise ValueError('Unexpected repository request destination')
    request=urllib.request.Request(url)
    username=os.environ.get('MAVEN_REPOSITORY_USERNAME','')
    password=os.environ.get('MAVEN_REPOSITORY_PASSWORD','')
    if repository()!='jitpack' and username and password:
        token=base64.b64encode(f'{username}:{password}'.encode()).decode()
        request.add_header('Authorization','Basic '+token)
    opener=urllib.request.build_opener(NoRepositoryRedirects())
    for attempt in range(3):
        try:
            with opener.open(request,timeout=20) as response:return response.read()
        except urllib.error.HTTPError as error:
            if error.code==404 and missing:return None
            if (error.code!=429 and error.code<500) or attempt==2:raise
        except (urllib.error.URLError,TimeoutError):
            if attempt==2:raise
        time.sleep(2**attempt)

def published_versions(group,artifact):
    if repository()=='maven-central' and repository_url()==common.CENTRAL:return common.published_versions(group,artifact)
    data=fetch(artifact_base(group,artifact)+'/maven-metadata.xml',missing=True)
    if data is None:return []
    xml=ET.fromstring(data)
    if xml.tag!='metadata' or xml.findtext('groupId')!=group or xml.findtext('artifactId')!=artifact:
        raise ValueError('Invalid repository metadata identity')
    versions=xml.findall('./versioning/versions/version')
    if not versions or any(not node.text for node in versions):raise ValueError('Malformed/empty repository metadata')
    return [node.text for node in versions]

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

def next_version(module,tags,published,requested=''):
    prefix=scope()+module+'/'
    selected=[t[len(prefix):] for t in tags if t.startswith(prefix)]
    stable=[v for v in published if re.fullmatch(common.SEMVER,v)]
    # Keep provenance checks even when the caller supplies a version.
    automatic=common.next_version(selected,published,'' if stable else '0.1.0')
    requested=requested.strip()
    if not requested:return automatic
    common.version_key(requested)
    if stable and common.version_key(requested)<=max(map(common.version_key,stable)):
        raise ValueError('Version must be newer than the latest release; use Finalize for recovery')
    return requested

def ensure_available(module,tags):
    legacy=[t for t in tags if re.fullmatch(r'release-(?:pending|uploading)/'+common.SEMVER,t)]
    if legacy and not scope(): raise ValueError('Unresolved legacy paired release; finalize it using the old workflow before independent releases')
    own=[t for t in tags if t.startswith((f'release-pending/{scope()}{module}/',f'release-uploading/{scope()}{module}/'))]
    if own: raise ValueError(f'Unresolved {module} attempt {own}; run finalization, never re-upload')

def validate_record(record,module,check_destination=True):
    target=record.get('repository','maven-central')
    if not isinstance(target,str) or not ID.fullmatch(target):raise ValueError('Invalid journal repository')
    if target!='maven-central' or 'repository_url' in record:validate_url(record.get('repository_url',''))
    if check_destination:
        if target!=repository():raise ValueError('Journal repository differs from selection')
        if record.get('repository_url',common.CENTRAL)!=repository_url():raise ValueError('Journal destination differs from configured URL')
    if module not in MODULES or record.get('module')!=module or (target!='jitpack' and record.get('artifact') not in ARTIFACTS[module]) or record.get('companions'):
        raise ValueError('Journal must contain exactly the selected module')
    common.version_key(record['version'])
    if not re.fullmatch('[0-9a-f]{40}',record['source']): raise ValueError('Invalid source')
    expected=set(common.SUFFIXES)
    actual=set(record['sha256'])
    valid_hashes=actual==expected or (target=='jitpack' and actual==expected-{'.module'})
    if not valid_hashes or any(not re.fullmatch('[0-9a-f]{64}',v) for v in record['sha256'].values()):
        raise ValueError('Incomplete publication hashes')
    if target=='jitpack': jitpack_release.artifact_base(record)
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
    read=fetch; base=artifact_base(group,artifact); endpoint=repository_url()
    if repository()!='maven-central' and version in common.published_versions(group,artifact):
        read=common.fetch; base=common.artifact_base(group,artifact); endpoint=common.CENTRAL
    elif repository()=='jitpack' or version not in published_versions(group,artifact):raise ValueError('Pinned model core dependency is not published')
    base=f'{base}/{version}/{artifact}-{version}'
    xml=ET.fromstring(read(base+'.pom'))
    for node in xml.iter(): node.tag=node.tag.split('}')[-1]
    if tuple(xml.findtext(k) for k in ('groupId','artifactId','version','packaging'))!=(group,artifact,version,'aar'):
        raise ValueError('Pinned core POM mismatch')
    if not read(base+'.aar'): raise ValueError('Pinned core AAR is unavailable')
    return endpoint

def all_records(remote=False):
    if remote:
        paths=git('ls-tree','-r','--name-only','origin/main','--','docs/releases').splitlines()
        return [json.loads(git('show',f'origin/main:{p}')) for p in paths if p.endswith('.json')]
    return [json.loads(p.read_text()) for p in (ROOT/'docs/releases').rglob('*.json')]

def prepare(module,requested=''):
    refresh(); tags=remote_tags(); ensure_available(module,tags)
    source=git('rev-parse','HEAD');git('merge-base','--is-ancestor',source,'origin/main')
    records=all_records(remote=True)
    for r in records:
        if r:validate_record(r,r['module'],check_destination=False)
    version,source=release_identity.select(module,source,git,tags,records,requested.strip())
    current=confirmed_record(module,remote=True)
    if current and current['version']==version:
        validate_record(current,module)
        if current['source']!=source:raise ValueError('Destination version has conflicting source')
        return common.outputs(dict(module=module,version=version,source=source,skip='true'))
    if repository()!='jitpack':
        group,artifact=identity(module)
        published=published_versions(group,artifact)
        if version in published:raise ValueError('Version exists at destination without a matching confirmed record; investigate before upload')
        if any(re.fullmatch(common.SEMVER,v) and common.version_key(v)>common.version_key(version) for v in published):
            raise ValueError('Destination has newer untracked releases; reconcile provenance')
    if module=='tfmodel': ensure_core_public(properties()['modelCoreVersion'])
    return common.outputs(dict(module=module,version=version,source=source,skip='false'))

def prepare_finalization(module,version,source=''):
    common.version_key(version)
    if source and not re.fullmatch('[0-9a-f]{40}',source): raise ValueError('Invalid source')
    refresh()
    if remote_ref(pending(module,version)):
        r=read_record(module,version)
        if source and source!=r['source']: raise ValueError('Conflicting source')
        git('merge-base','--is-ancestor',r['source'],'origin/main')
        current=confirmed_record(module,remote=True)
        if current and common.version_key(current['version'])>=common.version_key(version): raise ValueError('Pending release would overwrite newer documentation')
        return common.outputs(dict(module=module,version=version,source=r['source'],skip='false'))
    stable=tag(module,version)
    if not remote_ref(stable):stable=f'{scope()}{module}/v{version}'
    current=confirmed_record(module,remote=True)
    # Preserve legacy v0.1.0 provenance without inventing new migration tags.
    if current and current['version']==version and current.get('legacy_tag'): stable=current['legacy_tag']
    if not remote_ref(stable) or remote_ref(uploading(module,version)): raise ValueError('No matching pending or completed release')
    tagged=git('rev-parse',f'{stable}^{{commit}}')
    if source and tagged!=source: raise ValueError('Conflicting source')
    if not current:raise ValueError('Stable tag lacks confirmed docs')
    validate_record(current,module)
    if current['phase']!='confirmed-public' or common.version_key(current['version'])<common.version_key(version): raise ValueError('Stable tag lacks confirmed docs')
    current_tag=current.get('legacy_tag') or tag(module,current['version'])
    if not remote_ref(current_tag):current_tag=f'{scope()}{module}/v{current["version"]}'
    if not remote_ref(current_tag) or git('rev-parse',f'{current_tag}^{{commit}}')!=current['source']: raise ValueError('Confirmed source differs from stable tag')
    git('merge-base','--is-ancestor',tagged,current['source']);git('merge-base','--is-ancestor',current['source'],'origin/main')
    return common.outputs(dict(module=module,version=version,source=tagged,skip='true'))

def model_assets():
    return {'assets/'+p.relative_to(ROOT/'tfmodel/src/main/assets').as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'tfmodel/src/main/assets').rglob('*.tflite')}

def verify_bytes(data,suffix,r):
    common.verify_artifact(data,suffix,r['group'],r['artifact'],r.get('consumer_version',r['version']),model_core_version=r.get('core_version'),model_assets=r.get('model_assets'),model_core_artifact=r.get('core_artifact','core'),model_core_group=r.get('core_group'))

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
    r.update(repository=repository(),repository_url=repository_url())
    if repository()=='jitpack':
        group,artifact,consumer=jitpack_release.coordinates(module,version,artifact)
        r.update(group=group,artifact=artifact,consumer_version=consumer)
    if module=='tfmodel':
        r['core_version']=properties()['modelCoreVersion'];common.version_key(r['core_version'])
        r['core_artifact']=properties().get('modelCoreArtifact','core')
        r['core_group']=properties()['GROUP']
        r['core_repository_url']=ensure_core_public(r['core_version'])
        r['model_assets']=model_assets()
        if not r['model_assets']: raise ValueError('No model assets')
    consumer=r.get('consumer_version',version)
    directory=ROOT/'build/verification-repository'/group.replace('.','/')/artifact/consumer
    for suffix in common.SUFFIXES:
        data=(directory/f'{artifact}-{consumer}{suffix}').read_bytes();verify_bytes(data,suffix,r)
        if suffix=='-javadoc.jar':verify_documentation_contents(data)
        r['sha256'][suffix]=hashlib.sha256(data).hexdigest()
    validate_record(r,module);return r

def reserve(module,version,source):
    r=local_record(module,version,source)
    if module=='tfmodel':ensure_core_public(r['core_version'])
    base=f'{artifact_base(r["group"],r["artifact"])}/{version}/{r["artifact"]}-{version}'
    if repository()!='jitpack' and any(fetch(base+s,missing=True) is not None for s in common.SUFFIXES): raise ValueError('Immutable version already has public artifacts')
    refresh();ensure_available(module,remote_tags());git('merge-base','--is-ancestor',source,'origin/main')
    stable=tag(module,version)
    updates=[]
    if remote_ref(stable):
        if git('rev-parse',f'{stable}^{{commit}}')!=source:raise ValueError('Canonical tag has different source')
    else:
        git('tag','-a',stable,source,'-m',f'Module release {module} {version}; publication tracked separately')
        updates.append(f'refs/tags/{stable}')
    name=pending(module,version);git('tag','-a',name,source,'-m',json.dumps(r,sort_keys=True))
    git('push','--atomic','origin',*updates,f'refs/tags/{name}')

def guard(module,version):
    common.version_key(version);r=read_record(module,version)
    if r['source']!=git('rev-parse','HEAD') or (r['group'],r['artifact'])!=identity(module): raise ValueError('Reservation differs from source/module')
    if module=='tfmodel' and (r['core_version']!=properties()['modelCoreVersion'] or r.get('core_artifact','core')!=properties().get('modelCoreArtifact','core')): raise ValueError('Pinned core differs from reservation')
    current=confirmed_record(module,remote=True)
    if current and current['version']==version: raise ValueError('Version already finalized at destination')
    marker=uploading(module,version)
    if remote_ref(marker): raise ValueError('Upload already started; finalize without re-uploading')
    git('tag','-a',marker,r['source'],'-m',f'Upload may have started: {pending(module,version)}');git('push','origin',f'refs/tags/{marker}')

def verify_public(r):
    if repository()=='jitpack':
        # Requesting the artifact starts JitPack's build; API lookup alone does not.
        fetch(jitpack_release.artifact_base(r)+'.pom')
        return jitpack_release.verify(r,fetch)
    base=f'{artifact_base(r["group"],r["artifact"])}/{r["version"]}/{r["artifact"]}-{r["version"]}'
    for suffix in common.SUFFIXES:
        data=fetch(base+suffix);verify_bytes(data,suffix,r)
        if hashlib.sha256(data).hexdigest()!=r['sha256'][suffix]: raise ValueError('Public artifact hash mismatch')
        if b'BEGIN PGP SIGNATURE' not in fetch(base+suffix+'.asc'): raise ValueError('Missing signature')

def confirm(module,version,timeout=2400):
    common.version_key(version);r=read_record(module,version)
    if r['source']!=git('rev-parse','HEAD'): raise ValueError('Confirmation requires immutable source checkout')
    deadline=time.monotonic()+timeout
    while True:
        try:
            hashes=verify_public(r)
            if repository()=='jitpack':r['sha256']=hashes
            break
        except urllib.error.HTTPError as e:
            if e.code not in (404,429) and e.code<500: raise
            last=e
        except (urllib.error.URLError,TimeoutError) as e:last=e
        if time.monotonic()>=deadline: raise TimeoutError('Public artifacts not confirmed; preserve journal and retry finalization') from last
        time.sleep(min(20,max(0,deadline-time.monotonic())))
    r['phase']='confirmed-public'
    path=ROOT/f'build/{scope()}confirmed-{module}.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(r,indent=2,sort_keys=True)+'\n')
    return r

def confirmed_record(module,remote=False):
    path=metadata_path(module)
    if remote:
        if not git('ls-tree','--name-only','origin/main','--',path):return None
        return json.loads(git('show',f'origin/main:{path}'))
    local=ROOT/path
    return json.loads(local.read_text()) if local.exists() else None

def documentation(verify=False):
    # Preserve historical records even when a destination is later removed from the registry.
    targets=['maven-central']+sorted({p.parent.name for p in (ROOT/'docs/releases').glob('*/*.json')})
    records={(target,module):json.loads(path.read_text()) for target in targets for module in MODULES
             if (path:=ROOT/'docs/releases'/('' if target=='maven-central' else target)/f'{module}.json').exists()}
    sections=import_docs.render(records,{module:identity(module) for module in MODULES})
    template=(ROOT/'docs/templates/MODULE_IMPORT.md.template').read_text()
    expected=template.replace('{{INSTALLATION}}',sections)
    if '{{' in expected:raise ValueError('Unknown template placeholder')
    path=ROOT/'IMPORT.md'
    if verify:
        if path.read_text()!=expected:raise ValueError('IMPORT.md drift; run ./gradlew generateImportDocs')
    else:path.write_text(expected)

def finalize(module,version,source):
    common.version_key(version)
    if not re.fullmatch('[0-9a-f]{40}',source):raise ValueError('Invalid source')
    r=json.loads((ROOT/f'build/{scope()}confirmed-{module}.json').read_text());validate_record(r,module)
    if (r['version'],r['source'],r['phase'])!=(version,source,'confirmed-public'):raise ValueError('Confirmation mismatch')
    journal=read_record(module,version)
    expected={**journal,'phase':'confirmed-public'}
    if repository()=='jitpack':expected['sha256']=r['sha256']
    if expected!=r:raise ValueError('Confirmation differs from reservation')
    refresh();git('merge-base','--is-ancestor',source,'origin/main')
    # Artifact inputs may advance while a verified older release propagates.
    # Only changes to the executing protocol/renderer require reconciliation.
    git('diff','--exit-code',source,'origin/main','--','scripts',':(exclude)scripts/tests','publishing','docs/templates/MODULE_IMPORT.md.template')
    git('switch','-C',f'finalize-{module}','origin/main')
    current=confirmed_record(module)
    if current and common.version_key(current['version'])>=common.version_key(version):raise ValueError('Refusing to replace same/newer module metadata')
    (ROOT/metadata_path(module)).parent.mkdir(parents=True,exist_ok=True)
    (ROOT/metadata_path(module)).write_text(json.dumps(r,indent=2,sort_keys=True)+'\n');documentation()
    git('config','user.name','github-actions[bot]');git('config','user.email','41898282+github-actions[bot]@users.noreply.github.com')
    git('add',metadata_path(module),'IMPORT.md');git('commit','-m',f'docs: confirm {module} {version}')
    stable=tag(module,version)
    updates=['HEAD:refs/heads/main',f':refs/tags/{pending(module,version)}']
    if remote_ref(stable):
        if git('rev-parse',f'{stable}^{{commit}}')!=source:raise ValueError('Stable tag source mismatch')
    else:
        git('tag','-a',stable,source,'-m',f'Module release {module} {version}')
        updates.append(f'refs/tags/{stable}')
    if remote_ref(uploading(module,version)):
        if git('rev-parse',f'{uploading(module,version)}^{{commit}}')!=source:raise ValueError('Upload marker source mismatch')
        updates.append(f':refs/tags/{uploading(module,version)}')
    git('push','--atomic','origin',*updates)

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('command',choices=['prepare','prepare-finalization','check-local','reserve','guard','confirm','finalize','generate','verify'])
    p.add_argument('--module',choices=MODULES);p.add_argument('--version');p.add_argument('--source',default='');p.add_argument('--timeout',type=int,default=2400);a=p.parse_args()
    if a.command in ('generate','verify'):return documentation(a.command=='verify')
    if not a.module:p.error('--module is required')
    if a.command=='prepare':return prepare(a.module,a.version or '')
    if not a.version:p.error('--version is required')
    if a.command=='prepare-finalization':return prepare_finalization(a.module,a.version,a.source)
    if a.command in ('check-local','reserve','finalize') and not a.source:p.error('--source is required')
    if a.command=='check-local':print(json.dumps(local_record(a.module,a.version,a.source),indent=2))
    elif a.command=='reserve':reserve(a.module,a.version,a.source)
    elif a.command=='guard':guard(a.module,a.version)
    elif a.command=='confirm':confirm(a.module,a.version,a.timeout)
    elif a.command=='finalize':finalize(a.module,a.version,a.source)
if __name__=='__main__':main()
