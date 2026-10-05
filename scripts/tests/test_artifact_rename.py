import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from test_module_release import m
from test_release import artifacts

class RenameTests(unittest.TestCase):
    def test_current_config_with_historical_docs(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            (root/'docs/releases').mkdir(parents=True)
            (root/'docs/templates').mkdir()
            (root/'gradle.properties').write_text('GROUP=org.example\nPOM_ARTIFACT_ID=card-detector\nMODEL_ARTIFACT_ID=card-detector-model\nmodelCoreVersion=0.1.0\nmodelCoreArtifact=core\n')
            (root/'docs/templates/MODULE_IMPORT.md.template').write_text('{{INSTALLATION}}')
            for module,artifact in m.MODULES.items():
                r=dict(module=module,group='org.example',artifact=artifact,version='0.1.0',source='a'*40,sha256={s:'b'*64 for s in m.common.SUFFIXES},phase='confirmed-public')
                if module=='tfmodel':r['core_version']='0.1.0'
                (root/m.metadata_path(module)).write_text(json.dumps(r))
            with patch.object(m,'ROOT',root):
                self.assertEqual(m.identity('carddetector'),('org.example','card-detector'))
                m.documentation();m.documentation(verify=True)
                text=(root/'IMPORT.md').read_text()
                self.assertIn('org.example:core:0.1.0',text)
                self.assertIn('not yet confirmed',text)
                self.assertNotIn('implementation("org.example:card-detector:0.1.0")',text)

    def test_renamed_model_pom_preserves_explicit_core_pin(self):
        pom=artifacts('sentinel-card-model')['.pom'].replace(b'sentinel-card-model',b'card-detector-model')
        m.common.verify_pom(pom,'org.example','card-detector-model','1.2.3',model_core_version='1.2.3')
        renamed=pom.replace(b'<artifactId>core</artifactId>',b'<artifactId>card-detector</artifactId>')
        m.common.verify_pom(renamed,'org.example','card-detector-model','1.2.3',model_core_version='1.2.3',model_core_artifact='card-detector')
        with self.assertRaises(ValueError):m.common.verify_pom(renamed,'org.example','card-detector-model','1.2.3',model_core_version='1.2.3')

    def test_renamed_model_metadata_validates_dependency_artifact(self):
        data={'component':{'group':'org.example','module':'card-detector-model','version':'1.2.3'},'variants':[
            {'name':n,'dependencies':[{'group':'org.example','module':'card-detector','version':{'requires':'0.1.1'}}]} for n in ['api','runtime']]}
        m.common.verify_artifact(json.dumps(data).encode(),'.module','org.example','card-detector-model','1.2.3',model_core_version='0.1.1',model_core_artifact='card-detector')
        with self.assertRaises(ValueError):
            m.common.verify_artifact(json.dumps(data).encode(),'.module','org.example','card-detector-model','1.2.3',model_core_version='0.1.1',model_core_artifact='core')
