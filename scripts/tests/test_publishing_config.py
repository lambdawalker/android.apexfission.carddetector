"""Registry generation and runtime behavior for adding a third Maven target."""
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).parents[1]))
import publishing_config as config
import module_release as release

SAMPLE='''repositories:
  maven-central:
    environment: maven-central
    publisher: central
  internal:
    environment: internal-release
    publisher: maven
'''

class RegistryTests(unittest.TestCase):
    def test_new_repository_drives_workflow_options_and_gradle_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory); path=root/'repositories.yml';path.write_text(SAMPLE)
            entries=config.load_config(path)
            original='        # BEGIN GENERATED REPOSITORIES\n        options: [old]\n        # END GENERATED REPOSITORIES\n'
            result=config.render_dropdown(original,entries)
            self.assertEqual(config.yaml.safe_load(result)['options'],['maven-central','internal'])
            self.assertEqual(config.render_dropdown(result,entries),result)
            self.assertIn('internal.publisher=maven',config.render_properties(entries))
            self.assertIn('internal.environment=internal-release',config.render_properties(entries))

    def test_rejects_ambiguous_or_unsafe_configuration(self):
        for bad in [SAMPLE.replace('internal:', '../internal:'),SAMPLE.replace('publisher: maven','publisher: unknown'),SAMPLE.replace('internal-release','maven-central'),SAMPLE+'  internal:\n    environment: duplicate\n    publisher: maven\n']:
            with self.subTest(bad=bad),tempfile.TemporaryDirectory() as directory:
                path=Path(directory)/'repositories.yml';path.write_text(bad)
                with self.assertRaises(ValueError):config.load_config(path)

    def test_new_target_is_usable_without_precreated_confirmed_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'repositories.yml';path.write_text(SAMPLE)
            entries=config.load_config(path)
            with patch.object(release,'load_config',return_value=entries),patch.dict('os.environ',{'RELEASE_REPOSITORY':'internal','MAVEN_REPOSITORY_URL':'https://maven.example/releases'}):
                self.assertEqual(release.repository(),'internal')
                self.assertEqual(release.pending('tfmodel','0.1.0'),'release-pending/internal/tfmodel/0.1.0')
                self.assertEqual(release.next_version('tfmodel',[],[]),'0.1.0')
                with patch.object(release,'ROOT',Path(directory)):
                    self.assertIsNone(release.confirmed_record('tfmodel'))

    def test_every_repository_has_url_and_publisher_specific_secret_names(self):
        central=config.settings_for({'publisher':'central'})
        generic=config.settings_for({'publisher':'maven'})
        self.assertEqual(next(s for s in central if s['name']=='MAVEN_REPOSITORY_URL')['default'],'https://repo.maven.apache.org/maven2')
        self.assertIn('MAVEN_CENTRAL_USERNAME',[s['name'] for s in central])
        self.assertIn('MAVEN_REPOSITORY_USERNAME',[s['name'] for s in generic])

    def test_dropdown_ids_remain_strings_even_when_yaml_has_special_scalars(self):
        text = '# BEGIN GENERATED REPOSITORIES\noptions: []\n# END GENERATED REPOSITORIES\n'
        # Workflow indentation is intentional; only inspect the produced YAML values.
        text = ''.join('  ' + line for line in text.splitlines(keepends=True))
        result = config.render_dropdown(text, {'maven-central': {}, 'true': {}, 'null': {}})
        self.assertEqual(config.yaml.safe_load(result)['options'], ['maven-central','true','null'])
