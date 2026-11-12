"""JitPack entry point: only canonical module tags may publish one library.

Keep this standalone script compatible with Python 3.5 on JitPack's default
image. Other release tooling runs on the modern Python configured in Actions.
"""
import hashlib
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TAG = re.compile(r'(carddetector|tfmodel)[/~]v((?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*))(-jitpack)?')
MODEL_GLOB = 'tfmodel/src/main/assets/**/*.tflite'


def selection(value):
    match = TAG.fullmatch(value)
    if not match:
        raise ValueError('VERSION must be a canonical carddetector/vX.Y.Z or tfmodel/vX.Y.Z tag (slash or ~)')
    return match.group(1), match.group(2)


def run(*args):
    return subprocess.check_output(args, cwd=str(ROOT), universal_newlines=True).strip()


def verify_models():
    paths = run('git', 'ls-files', '--', MODEL_GLOB).splitlines()
    if not paths:
        raise ValueError('No tracked model assets')
    for path in paths:
        data = (ROOT / path).read_bytes()
        if len(data) < 8 or data[4:8] != b'TFL3':
            raise ValueError('Model is missing, invalid, or an LFS pointer: {}'.format(path))
        blob = subprocess.check_output(['git', 'show', 'HEAD:{}'.format(path)], cwd=str(ROOT))
        if blob.startswith(b'version https://git-lfs.github.com/spec/v1\n'):
            pointer = blob.decode().splitlines()
            oid = next((s[11:] for s in pointer if s.startswith('oid sha256:')), '')
            size = next((s[5:] for s in pointer if s.startswith('size ')), '')
            if hashlib.sha256(data).hexdigest() != oid or str(len(data)) != size:
                raise ValueError('Model differs from committed LFS object: {}'.format(path))
        elif data != blob:
            raise ValueError('Model differs from committed asset: {}'.format(path))


def command(module, version, scoped=False):
    selection('{}/v{}'.format(module, version))
    return ['./gradlew', '--no-daemon', ':{}:publishJitPack'.format(module),
            '-PjitpackBuild=true', '-PreleaseModule={}'.format(module), '-PreleaseVersion={}'.format(version),
            '-PjitpackScopedTag={}'.format(str(scoped).lower())]


def main():
    requested_tag = os.environ.get('VERSION', '')
    module, version = selection(requested_tag)
    tag = requested_tag.replace('~', '/')
    source = run('git', 'rev-parse', 'HEAD')
    if run('git', 'rev-parse', 'refs/tags/{}^{{commit}}'.format(tag)) != source:
        raise ValueError('Canonical tag does not identify checkout')
    if os.environ.get('GIT_COMMIT', source) != source:
        raise ValueError('JitPack GIT_COMMIT differs from checkout')
    if module == 'tfmodel':
        # Never fetch documentation recordings, screenshots, or unrelated LFS data.
        subprocess.run(['git', 'lfs', 'pull', '--include=' + MODEL_GLOB, '--exclude='], cwd=str(ROOT), check=True)
        verify_models()
    subprocess.run(command(module, version, tag.endswith('-jitpack')), cwd=str(ROOT), check=True)


if __name__ == '__main__':
    main()
