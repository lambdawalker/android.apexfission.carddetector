import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release_identity as identity

class IdentityTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git('init','-q'); self.git('config','user.email','test@example.com'); self.git('config','user.name','test')
        self.write('carddetector/src/main/code.kt','one')
        self.write('tfmodel/src/main/code.kt','one')
        self.write('gradle.properties','GROUP=org.test\nmodelCoreVersion=0.1.0\n')
        self.commit(); self.source = self.git('rev-parse','HEAD')
        self.git('tag','carddetector/v0.1.5')
    def git(self,*args):
        return subprocess.check_output(['git',*args],cwd=self.root,text=True,stderr=subprocess.DEVNULL).strip()
    def write(self,path,value):
        p=self.root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(value)
    def commit(self):self.git('add','.');self.git('commit','-qm','change')
    def choose(self,module='carddetector',requested=''):
        return identity.select(module,self.git('rev-parse','HEAD'),self.git,self.git('tag').splitlines(),[],requested)
    def test_same_source_reuses_global_version(self):
        self.assertEqual(('0.1.5',self.source),self.choose())
    def test_generated_docs_and_sibling_source_reuse_original_commit(self):
        self.write('IMPORT.md','updated'); self.write('tfmodel/src/main/code.kt','two');self.commit()
        self.assertEqual(('0.1.5',self.source),self.choose())
    def test_changed_library_bumps_global_not_destination(self):
        self.write('carddetector/src/main/code.kt','two');self.commit()
        self.assertEqual(('0.1.6',self.git('rev-parse','HEAD')),self.choose())
    def test_shared_build_change_bumps(self):
        self.write('gradle/libs.versions.toml','kotlin=new');self.commit()
        self.assertEqual('0.1.6',self.choose()[0])
    def test_model_pin_does_not_bump_detector(self):
        self.write('gradle.properties','GROUP=org.test\nmodelCoreVersion=0.2.0\n');self.commit()
        self.assertEqual(('0.1.5',self.source),self.choose())
    def test_conflicting_destination_tag_rejected(self):
        self.write('carddetector/src/main/code.kt','two');self.commit();self.git('tag','apexfission-maven/carddetector/v0.1.5')
        with self.assertRaisesRegex(ValueError,'Conflicting'):self.choose()
    def test_explicit_lower_version_rejected(self):
        with self.assertRaises(ValueError):self.choose(requested='0.1.4')
    def test_explicit_major_version_allowed(self):
        self.assertEqual(('1.0.0',self.source),self.choose(requested='1.0.0'))
    def test_older_checkout_not_released_again(self):
        self.write('carddetector/src/main/code.kt','two');self.commit();self.git('tag','carddetector/v0.1.6');self.git('checkout',self.source)
        with self.assertRaisesRegex(ValueError,'ancestor'):self.choose()

    def test_legacy_pending_other_destination_reserves_global_version(self):
        self.git('tag','release-pending/apexfission-maven/carddetector/0.1.9')
        self.assertEqual(('0.1.9',self.source),self.choose())
