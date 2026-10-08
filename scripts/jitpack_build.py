"""JitPack entry point: only canonical module tags may publish one library."""
import hashlib
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TAG = re.compile(r'(carddetector|tfmodel)[/~]v((?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*))')
MODEL_GLOB = 'tfmodel/src/main/assets/**/*.tflite'


def selection(value):
    match = TAG.fullmatch(value)
    if not match:
        raise ValueError('VERSION must be a canonical carddetector/vX.Y.Z or tfmodel/vX.Y.Z tag (slash or ~)')
    return match.groups()


def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()


def verify_models():
    paths = run('git', 'ls-files', '--', MODEL_GLOB).splitlines()
    if not paths:
        raise ValueError('No tracked model assets')
    for path in paths:
        data = (ROOT / path).read_bytes()
        if len(data) < 8 or data[4:8] != b'TFL3':
            raise ValueError(f'Model is missing, invalid, or an LFS pointer: {path}')
        blob = subprocess.check_output(['git', 'show', f'HEAD:{path}'], cwd=ROOT)
        if blob.startswith(b'version https://git-lfs.github.com/spec/v1\n'):
            pointer = blob.decode().splitlines()
            oid = next((s[11:] for s in pointer if s.startswith('oid sha256:')), '')
            size = next((s[5:] for s in pointer if s.startswith('size ')), '')
            if hashlib.sha256(data).hexdigest() != oid or str(len(data)) != size:
                raise ValueError(f'Model differs from committed LFS object: {path}')
        elif data != blob:
            raise ValueError(f'Model differs from committed asset: {path}')


def command(module, version):
    selection(f'{module}/v{version}')
    return ['./gradlew', '--no-daemon', f':{module}:publishToMavenLocal',
            '-PjitpackBuild=true', f'-PreleaseModule={module}', f'-PreleaseVersion={version}']


def main():
    module, version = selection(os.environ.get('VERSION', ''))
    source = run('git', 'rev-parse', 'HEAD')
    if run('git', 'rev-parse', f'refs/tags/{module}/v{version}^{{commit}}') != source:
        raise ValueError('Canonical tag does not identify checkout')
    if os.environ.get('GIT_COMMIT', source) != source:
        raise ValueError('JitPack GIT_COMMIT differs from checkout')
    if module == 'tfmodel':
        # Never fetch documentation recordings, screenshots, or unrelated LFS data.
        subprocess.run(['git', 'lfs', 'pull', '--include=' + MODEL_GLOB, '--exclude='], cwd=ROOT, check=True)
        verify_models()
    subprocess.run(command(module, version), cwd=ROOT, check=True)


if __name__ == '__main__':
    main()
