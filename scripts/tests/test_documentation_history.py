"""History must survive latest-pointer replacement and destination catch-up."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import documentation_history as h

class HistoryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
    def record(self, version='0.1.2', target='maven-central', source='a'*40):
        return dict(module='carddetector', version=version, source=source, phase='confirmed-public',
                    repository=target, group='org.example', artifact='detector',
                    **({'consumer_version':f'carddetector~v{version}','repository_url':'https://jitpack.io'} if target=='jitpack' else {}))
    def test_archive_retains_old_versions_and_merges_mirrors(self):
        first=h.archive_record(self.root,self.record())
        h.archive_record(self.root,self.record('0.1.3',source='b'*40))
        h.archive_record(self.root,self.record(target='jitpack'))
        records=h.load_catalog(self.root)
        self.assertEqual(len(records),2)
        old=json.loads((self.root/first).read_text())
        self.assertEqual(set(old['destinations']),{'maven-central','jitpack'})
        self.assertEqual(old['documentation_ref'],'a'*40)
        before=(self.root/first).read_bytes()
        h.archive_record(self.root,self.record())
        self.assertEqual((self.root/first).read_bytes(),before)
    def test_conflicting_identity_or_changed_confirmation_rejected(self):
        h.archive_record(self.root,self.record())
        for record in [self.record(target='jitpack',source='b'*40),dict(self.record(),artifact='other')]:
            with self.assertRaisesRegex(ValueError,'conflict'):h.archive_record(self.root,record)
    def test_pending_and_unsafe_inputs_rejected(self):
        for record in [dict(self.record(),phase='reserved-before-upload'),dict(self.record(),version='../escape'),dict(self.record(),source='main')]:
            with self.assertRaises(ValueError):h.archive_record(self.root,record)
    def test_seed_ignores_null_destinations_and_keeps_history(self):
        folder=self.root/'docs/releases/jitpack';folder.mkdir(parents=True)
        (folder/'carddetector.json').write_text(json.dumps(self.record(target='jitpack')))
        (folder/'tfmodel.json').write_text('null')
        h.seed(self.root)
        (folder/'carddetector.json').write_text(json.dumps(self.record('0.1.3','jitpack','b'*40)))
        h.seed(self.root)
        self.assertEqual([r['version'] for r in h.load_catalog(self.root)],['0.1.2','0.1.3'])
    def test_installation_is_exact_version_and_spanish_preserves_code(self):
        h.archive_record(self.root,self.record())
        record=h.load_catalog(self.root)[0]
        en=h.installation(record,'en');es=h.installation(record,'es')
        self.assertIn('org.example:detector:0.1.2',en)
        self.assertIn('Instalación',es)
        import re
        self.assertEqual(re.findall(r'```[\s\S]*?```',en),re.findall(r'```[\s\S]*?```',es))
    def test_verify_detects_missing_latest_record(self):
        folder=self.root/'docs/releases';folder.mkdir(parents=True)
        (folder/'carddetector.json').write_text(json.dumps(self.record()))
        with self.assertRaisesRegex(ValueError,'archive'):h.verify(self.root)
        h.seed(self.root);h.verify(self.root)
