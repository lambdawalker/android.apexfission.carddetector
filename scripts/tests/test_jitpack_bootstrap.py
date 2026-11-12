"""Exercise the JitPack shell bootstrap without installing or publishing anything."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import textwrap
import unittest

ROOT = Path(__file__).resolve().parents[2]

@unittest.skipIf(os.name == 'nt', 'JitPack runs on Linux')
class JitpackBootstrapTests(unittest.TestCase):
    def test_pinned_cli_is_installed_and_delegates_through_setup(self):
        script = textwrap.dedent((ROOT / 'jitpack.yml').read_text().split('  - |\n', 1)[1])
        for pin in ('v0.1.14', "'v0.1.14'"):
            with self.subTest(pin=pin), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / '.project/setup').mkdir(parents=True)
                (root / '.project/setup/config.yaml').write_text(f'tooling_ref: {pin}\n')
                shutil.copy(ROOT / 'setup.sh', root / 'setup.sh')
                cargo = root / 'cargo/bin'
                cargo.mkdir(parents=True)
                (cargo / 'cargo').write_text('#!/bin/sh\nprintf "%s\\n" "$@" > cargo-args\n')
                (cargo / 'android-repo-setup').write_text('#!/bin/sh\nif [ "$1" = --version ]; then echo "android-repo-setup 0.1.14"; else printf "%s\\n" "$@" > cli-args; fi\n')
                for path in cargo.iterdir():
                    path.chmod(0o755)
                env = dict(os.environ, CARGO_HOME=str(root / 'cargo'), PATH=str(cargo) + os.pathsep + os.environ['PATH'])
                result = subprocess.run(['sh', '-c', script], cwd=root, env=env, capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual((root / 'cargo-args').read_text().splitlines(), ['install', '--locked', '--git', 'https://github.com/lambdawalker/gradle.android-repo-setup.git', '--tag', 'v0.1.14', 'android-repo-setup'])
                self.assertEqual((root / 'cli-args').read_text().splitlines(), ['--project', str(root), 'jitpack_build'])
