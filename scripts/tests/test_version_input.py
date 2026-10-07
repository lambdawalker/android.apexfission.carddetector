import unittest
from test_module_release import m

class VersionInputTests(unittest.TestCase):
    def test_default_first(self):
        self.assertEqual(m.next_version('carddetector',[],[]),'0.1.0')
    def test_auto_patch(self):
        self.assertEqual(m.next_version('carddetector',['carddetector/v1.2.9'],['1.2.9']),'1.2.10')
    def test_explicit_version(self):
        self.assertEqual(m.next_version('carddetector',[],[],'2.0.0'),'2.0.0')
        self.assertEqual(m.next_version('carddetector',['carddetector/v1.2.9'],['1.2.9'],'2.0.0'),'2.0.0')
    def test_reuse_and_downgrade_rejected(self):
        for version in ['1.2.9','1.2.8','bad','1.3.0-SNAPSHOT']:
            with self.assertRaises(ValueError):m.next_version('carddetector',['carddetector/v1.2.9'],['1.2.9'],version)
    def test_override_does_not_bypass_provenance(self):
        with self.assertRaises(ValueError):m.next_version('carddetector',['carddetector/v1.2.9'],[],'2.0.0')
