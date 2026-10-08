"""Installation selection and examples must never imply an unconfirmed publication."""
import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]))
from import_docs import render, CENTRAL

IDENTITIES = {'carddetector': ('org.example', 'card-detector'),
              'tfmodel': ('org.example', 'card-detector-model')}


def record(module='carddetector', target='maven-central', version='1.2.3', **changes):
    result = dict(module=module, group='org.example', artifact=IDENTITIES[module][1],
                  version=version, source='a' * 40, phase='confirmed-public')
    if module == 'tfmodel':
        result.update(core_version='0.1.0', core_artifact='core')
    if target != 'maven-central':
        result.update(repository=target, repository_url='https://jitpack.io',
                      group='com.github.lambdawalker', artifact='android.apexfission.carddetector',
                      consumer_version=f'{module}~v{version}')
    result.update(changes)
    return result


class InstallationTests(unittest.TestCase):
    def test_empty_records_explicitly_report_both_modules(self):
        self.assertEqual(render({}, IDENTITIES).count('No confirmed release.'), 2)
        self.assertNotIn('implementation(', render({}, IDENTITIES))

    def test_latest_semantic_release_hides_older_destinations(self):
        records = {('maven-central', 'carddetector'): record(version='1.9.9'),
                   ('jitpack', 'carddetector'): record(target='jitpack', version='1.10.0')}
        text = render(records, IDENTITIES)
        self.assertIn('**1.10.0**', text)
        self.assertIn('carddetector~v1.10.0', text)
        self.assertNotIn('1.9.9', text)
        self.assertNotIn('### maven-central', text)

    def test_equal_release_destinations_require_same_source(self):
        records = {('maven-central', 'carddetector'): record(),
                   ('jitpack', 'carddetector'): record(target='jitpack')}
        text = render(records, IDENTITIES)
        self.assertIn('### maven-central', text)
        self.assertIn('### jitpack', text)
        self.assertIn('Choose **one** destination', text)
        records[('jitpack', 'carddetector')]['source'] = 'b' * 40
        with self.assertRaisesRegex(ValueError, 'Conflicting confirmed sources'):
            render(records, IDENTITIES)

    def test_newer_pending_never_replaces_confirmed(self):
        records = {('maven-central', 'carddetector'): record(),
                   ('jitpack', 'carddetector'): record(target='jitpack', version='9.0.0', phase='reserved-before-upload')}
        text = render(records, IDENTITIES)
        self.assertIn('**1.2.3**', text)
        self.assertNotIn('9.0.0', text)
        self.assertNotIn('### jitpack', text)
        self.assertEqual(render({('jitpack', 'carddetector'): records[('jitpack', 'carddetector')]}, IDENTITIES).count('No confirmed release.'), 2)

    def test_unknown_phase_rejected(self):
        with self.assertRaisesRegex(ValueError, 'confirmed releases'):
            render({('maven-central', 'carddetector'): record(phase='uploaded')}, IDENTITIES)

    def test_each_module_selects_its_own_latest_version(self):
        text = render({('jitpack', 'carddetector'): record(target='jitpack', version='2.0.0'),
                       ('maven-central', 'tfmodel'): record(module='tfmodel', version='1.0.0')}, IDENTITIES)
        self.assertIn('**2.0.0**', text)
        self.assertIn('**1.0.0**', text)
        self.assertIn('Exports `org.example:core:0.1.0`', text)

    def test_examples_include_actual_consumer_version_and_dependency_repository(self):
        r = record(module='tfmodel', target='jitpack', core_group='org.pinned',
                   core_consumer_version='carddetector~v0.1.0', core_repository_url='https://packages.example.com/releases')
        text = render({('jitpack', 'tfmodel'): r}, IDENTITIES)
        self.assertIn('Exports `org.pinned:core:carddetector~v0.1.0`', text)
        self.assertIn('implementation("com.github.lambdawalker:android.apexfission.carddetector:tfmodel~v1.2.3")', text)
        self.assertIn('version = "tfmodel~v1.2.3"', text)
        self.assertIn('<version>tfmodel~v1.2.3</version>', text)
        self.assertIn('implementation(libs.tfmodel)', text)
        for url in ('https://jitpack.io', 'https://packages.example.com/releases'):
            self.assertIn(f'maven {{ url = uri("{url}") }}', text)
            self.assertIn(f"maven {{ url '{url}' }}", text)
            self.assertIn(f'<url>{url}</url>', text)
        self.assertIn('google()', text)
        self.assertIn('mavenCentral()', text)
        self.assertIn(f'<url>{CENTRAL}</url>', text)

    def test_legacy_model_pin_remains_exact(self):
        text = render({('maven-central', 'tfmodel'): record(module='tfmodel', artifact='sentinel-card-model')}, IDENTITIES)
        self.assertIn('Exports `org.example:core:0.1.0`', text)
        self.assertIn('not yet confirmed', text)
        self.assertNotIn('Exports `org.example:card-detector:', text)

    def test_mismatched_record_location_rejected(self):
        with self.assertRaises(ValueError):
            render({('jitpack', 'carddetector'): record()}, IDENTITIES)

    def test_unsafe_metadata_cannot_be_injected_into_examples(self):
        changes = [dict(group='org.example"'), dict(artifact='bad\n```'),
                   dict(source='[bad](url)'), dict(consumer_version='${code}'),
                   dict(repository_url="https://example.com/'code'"),
                   dict(repository_url='https://example.com/$code'),
                   dict(repository_url='https://example.com/a&b'),
                   dict(core_group='org.${bad}'), dict(core_artifact='<bad>'),
                   dict(core_repository_url='https://example.com/`code`')]
        for change in changes:
            with self.subTest(change=change), self.assertRaises(ValueError):
                render({('maven-central', 'tfmodel'): record(module='tfmodel', **change)}, IDENTITIES)

    def test_jitpack_tag_must_match_semantic_identity(self):
        with self.assertRaisesRegex(ValueError, 'consumer version'):
            render({('jitpack', 'carddetector'): record(target='jitpack', consumer_version='main-SNAPSHOT')}, IDENTITIES)


if __name__ == '__main__':
    unittest.main()
