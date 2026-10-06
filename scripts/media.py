#!/usr/bin/env python3
"""Historical recording assets: render candidates, explicitly accept, verify integrity."""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
VIDEO = ROOT/'docs/demo.mp4'
ACCEPTED = ROOT/'docs/screenshots/tracking.png'
CANDIDATE = ROOT/'build/docs-media/tracking.png'
MANIFEST = ROOT/'docs/screenshots/manifest.json'

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def run(*args): return subprocess.check_output(args, text=True).strip()
def main():
    p=argparse.ArgumentParser();p.add_argument('action',choices=['render','accept','verify','compare']);args=p.parse_args()
    if not VIDEO.exists() or VIDEO.stat().st_size < 1000000: raise SystemExit('Hydrate the recording with git lfs pull; missing or pointer-only MP4')
    if args.action in ['render','compare']:
        CANDIDATE.parent.mkdir(parents=True,exist_ok=True)
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-ss','12','-i',str(VIDEO),'-frames:v','1','-vf','scale=540:-1','-threads','1',str(CANDIDATE)],check=True)
        evidence={'source_sha256':sha(VIDEO),'output_sha256':sha(CANDIDATE),'tool':run('ffmpeg','-version').splitlines()[0]}
        CANDIDATE.with_suffix('.json').write_text(json.dumps(evidence,indent=2)+'\n')
        if args.action=='render': print('Inspect candidate:',CANDIDATE);return
        if not ACCEPTED.exists() or sha(ACCEPTED)!=sha(CANDIDATE): raise SystemExit('Candidate differs; inspect it, never auto-accept in CI')
        print('Candidate equals accepted historical frame');return
    if args.action=='accept':
        evidence=json.loads(CANDIDATE.with_suffix('.json').read_text())
        if evidence['source_sha256']!=sha(VIDEO) or evidence['output_sha256']!=sha(CANDIDATE): raise SystemExit('Stale candidate; render again')
        shutil.copyfile(CANDIDATE,ACCEPTED)
        data={'schema':1,'recording':{'path':str(VIDEO.relative_to(ROOT)),'sha256':sha(VIDEO),'duration_seconds':30.733456,'width':1080,'height':2340,'capture_commit':None,'capture_configuration':None,'repository_input_commit':run('git','-C',str(ROOT),'log','-1','--format=%H','--',str(VIDEO.relative_to(ROOT))),'scope':'Historical user-supplied recording; original capture ref and device unknown; not release evidence.'},'scenarios':[{'id':'historical-card-tracking','file':'tracking.png','sha256':sha(ACCEPTED),'capture_kind':'recording-frame','fixture':'docs/demo.mp4','timestamp_seconds':12,'render':{'filter':'scale=540:-1','threads':1,'tool':evidence['tool']},'alt':'Cyan tracking guide around a sample card at 12 seconds in the historical recording','guide':'docs/agents/demos.md','demo':'app/src/main/java/com/apexfission/android/carddetector/demo/CardDetectionActivity.kt'}]}
        MANIFEST.write_text(json.dumps(data,indent=2)+'\n');print('Accepted inspected candidate; review manifest and image diff');return
    data=json.loads(MANIFEST.read_text())
    if sha(VIDEO)!=data['recording']['sha256']: raise SystemExit('Recording changed; review provenance and regenerate intentionally')
    for item in data['scenarios']:
        path=ACCEPTED.parent/item['file']
        if not path.exists() or sha(path)!=item['sha256']: raise SystemExit('Missing or changed accepted image: '+str(path))
        for key in ['fixture','guide','demo']:
            if not (ROOT/item[key]).exists(): raise SystemExit('Missing manifest target '+item[key])
    if not any(x['id']=='historical-card-tracking' for x in data['scenarios']): raise SystemExit('Required showcase scenario missing')
    print('Historical recording and accepted frame integrity verified; no claim of current device capture')
if __name__=='__main__': main()
