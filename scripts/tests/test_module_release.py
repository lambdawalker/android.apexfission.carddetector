"""Independent release history, dependency contracts and module-scoped retry state."""
import importlib.util
import sys
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from test_release import artifacts
sys.path.insert(0,str(Path(__file__).parents[1]))
import module_release as m

class ModulePolicyTests(unittest.TestCase):
    def test_tags_only_affect_selected_module(self):
        self.assertEqual(m.next_version('carddetector', ['carddetector/v0.1.1','tfmodel/v8.0.0'], ['0.1.0','0.1.1']), '0.1.2')
        self.assertEqual(m.next_version('tfmodel', ['carddetector/v9.0.0'], ['0.1.0']), '0.1.1')

    def test_legacy_tags_are_not_reassigned_to_new_module_history(self):
        self.assertEqual(m.next_version('tfmodel',['v9.0.0'],['0.1.0']), '0.1.1')

    def test_pending_sibling_does_not_block(self):
        m.ensure_available('carddetector',['release-pending/tfmodel/0.1.1'])
        with self.assertRaisesRegex(ValueError,'Unresolved'):
            m.ensure_available('tfmodel',['release-pending/tfmodel/0.1.1'])
        with self.assertRaisesRegex(ValueError,'legacy'):
            m.ensure_available('carddetector',['release-pending/0.1.1'])

    def test_single_publication_record_not_a_pair(self):
        data=dict(module='carddetector',group='com.apexfission.android.carddetector',artifact='core',version='0.1.1',source='a'*40,sha256={s:'b'*64 for s in m.common.SUFFIXES},phase='reserved-before-upload')
        m.validate_record(data,'carddetector')
        with self.assertRaises(ValueError):m.validate_record(data,'tfmodel')
        data['companions']=[{}]
        with self.assertRaises(ValueError):m.validate_record(data,'carddetector')

    def test_model_pom_allows_older_pinned_core(self):
        pom=artifacts('sentinel-card-model')['.pom'].replace(b'<artifactId>core</artifactId><version>1.2.3</version>',b'<artifactId>core</artifactId><version>0.1.0</version>')
        m.common.verify_pom(pom,'org.example','sentinel-card-model','1.2.3',model_core_version='0.1.0')
        with self.assertRaises(ValueError):m.common.verify_pom(pom,'org.example','sentinel-card-model','1.2.3',model_core_version='0.2.0')

    def test_model_module_metadata_checks_both_consumption_variants(self):
        data={'component':{'group':'org.example','module':'sentinel-card-model','version':'1.2.3'},'variants':[
            {'name':n,'dependencies':[{'group':'org.example','module':'core','version':{'requires':'0.1.0'}}]} for n in ['releaseApiElements','releaseRuntimeElements']]}
        m.common.verify_artifact(json.dumps(data).encode(),'.module','org.example','sentinel-card-model','1.2.3',model_core_version='0.1.0')
        data['variants'][1]['dependencies'][0]['version']['requires']='1.2.3'
        with self.assertRaises(ValueError):m.common.verify_artifact(json.dumps(data).encode(),'.module','org.example','sentinel-card-model','1.2.3',model_core_version='0.1.0')
