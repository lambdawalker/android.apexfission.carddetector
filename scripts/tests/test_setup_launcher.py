"""The POSIX launcher must honor the project pin, arguments and exit status."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]

@unittest.skipIf(os.name == 'nt', 'POSIX launcher; Windows has a real-binary CI smoke test')
class SetupLauncherTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.project = Path(self.temp.name) / 'project with spaces'
        self.config = self.project / '.project/setup/config.yaml'
        self.config.parent.mkdir(parents=True)
        self.config.write_text("tooling_ref: 'v0.1.12'\n")
        shutil.copyfile(ROOT / 'setup.sh', self.project / 'setup.sh')
        self.bin = Path(self.temp.name) / 'tools'
        self.bin.mkdir()
        self.output = Path(self.temp.name) / 'arguments.json'
        executable = self.bin / 'android-repo-setup'
        executable.write_text('''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
if sys.argv[1:] == ['--version']:
    print(os.environ.get('TEST_VERSION', 'android-repo-setup 0.1.12'))
else:
    Path(os.environ['TEST_ARGUMENTS']).write_text(json.dumps(sys.argv[1:]))
    sys.exit(7)
''')
        executable.chmod(0o755)
        self.env = dict(os.environ, PATH=str(self.bin)+os.pathsep+os.environ['PATH'], TEST_ARGUMENTS=str(self.output))
    def run_launcher(self):
        return subprocess.run(['sh', str(self.project/'setup.sh'), 'workflows', 'argument with spaces'], cwd=self.temp.name, env=self.env, text=True, capture_output=True)
    def test_project_directory_arguments_and_exit_code(self):
        result = self.run_launcher()
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertEqual(json.loads(self.output.read_text()), ['--project',str(self.project),'workflows','argument with spaces'])
    def test_wrong_version_is_rejected_before_execution(self):
        self.env['TEST_VERSION'] = 'android-repo-setup 0.1.11'
        result = self.run_launcher()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Expected android-repo-setup 0.1.12', result.stderr)
        self.assertFalse(self.output.exists())
    def test_invalid_pin_is_rejected_before_execution(self):
        self.config.write_text('tooling_ref: main\n')
        self.assertNotEqual(self.run_launcher().returncode, 0)
        self.assertFalse(self.output.exists())
