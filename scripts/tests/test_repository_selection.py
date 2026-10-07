"""Repository selection must isolate release history and immutable destinations."""
import os
import unittest
from unittest.mock import patch
import module_release as m

HOST = {'RELEASE_REPOSITORY': 'apexfission-maven', 'MAVEN_REPOSITORY_URL': 'https://maven.example/releases/'}

class RepositoryTests(unittest.TestCase):
    def test_self_hosted_history_does_not_consume_central_versions_or_attempts(self):
        with patch.dict(os.environ, HOST):
            self.assertEqual(m.next_version('carddetector', ['carddetector/v9.0.0'], []), '0.1.0')
            m.ensure_available('carddetector', ['release-pending/carddetector/0.1.0'])
            with self.assertRaisesRegex(ValueError, 'Unresolved'):
                m.ensure_available('carddetector', ['release-pending/apexfission-maven/carddetector/0.1.0'])
            self.assertEqual(m.next_version('carddetector', ['apexfission-maven/carddetector/v1.2.3'], ['1.2.3']), '1.2.4')
            self.assertEqual(m.metadata_path('carddetector'), 'docs/releases/apexfission-maven/carddetector.json')

    def test_central_ignores_self_hosted_attempts(self):
        with patch.dict(os.environ, {'RELEASE_REPOSITORY':'maven-central'}):
            m.ensure_available('carddetector', ['release-pending/apexfission-maven/carddetector/0.1.0'])
            self.assertEqual(m.tag('carddetector','0.1.0'), 'carddetector/v0.1.0')

    def test_destination_is_pinned_in_journal(self):
        r=dict(module='carddetector',group='org.example',artifact='card-detector',version='0.1.0',source='a'*40,sha256={s:'b'*64 for s in m.common.SUFFIXES},phase='reserved-before-upload',repository='apexfission-maven',repository_url='https://maven.example/releases')
        with patch.dict(os.environ, HOST):
            m.validate_record(r,'carddetector')
            with patch.dict(os.environ, {'MAVEN_REPOSITORY_URL':'https://other.example/releases'}):
                with self.assertRaisesRegex(ValueError,'destination'):
                    m.validate_record(r,'carddetector')
        with patch.dict(os.environ, {'RELEASE_REPOSITORY':'maven-central'}):
            with self.assertRaisesRegex(ValueError,'repository'):
                m.validate_record(r,'carddetector')

    def test_url_required_and_rejects_credentials_query_and_plain_http(self):
        with patch.dict(os.environ, HOST):
            for url in ['', 'http://maven.example/releases', 'https://user:password@maven.example/releases', 'https://maven.example/releases?token=x', 'https://maven.example/#secret']:
                with self.subTest(url=url), patch.dict(os.environ, {'MAVEN_REPOSITORY_URL':url}):
                    with self.assertRaises(ValueError): m.repository_url()
            self.assertEqual(m.repository_url(), 'https://maven.example/releases')

    def test_metadata_uses_selected_endpoint_and_basic_auth(self):
        import io
        seen=[]
        class Transport:
            def open(self, request, timeout):
                seen.append((request.full_url,request.get_header('Authorization')))
                return io.BytesIO(b'<metadata><groupId>org.example</groupId><artifactId>card-detector</artifactId><versioning><versions><version>0.1.0</version></versions></versioning></metadata>')
        with patch.dict(os.environ,{**HOST,'MAVEN_REPOSITORY_USERNAME':'reader','MAVEN_REPOSITORY_PASSWORD':'password'}), patch.object(m.urllib.request,'build_opener',return_value=Transport()):
            self.assertEqual(m.published_versions('org.example','card-detector'), ['0.1.0'])
        self.assertEqual(seen,[('https://maven.example/releases/org/example/card-detector/maven-metadata.xml','Basic cmVhZGVyOnBhc3N3b3Jk')])

    def test_credentials_are_never_sent_to_other_endpoint_or_redirect(self):
        with patch.dict(os.environ,HOST):
            with self.assertRaisesRegex(ValueError,'destination'):m.fetch('https://other.example/file.pom')
        with self.assertRaisesRegex(ValueError,'redirected'):
            m.NoRepositoryRedirects().redirect_request(None,None,302,'Found',{},'https://other.example/file.pom')
