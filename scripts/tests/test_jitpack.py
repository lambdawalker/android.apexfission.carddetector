import hashlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import jitpack_build as build
import jitpack_release as jp


def zipped(entries):
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, 'w') as archive:
        for name, data in entries.items():
            archive.writestr(name, data)
    return stream.getvalue()


class JitpackTests(unittest.TestCase):
    def test_selection(self):
        for separator in ('/', '~'):
            self.assertEqual(build.selection(f'tfmodel{separator}v1.2.3'), ('tfmodel', '1.2.3'))
        for value in ('master', 'v1.2.3', 'app/v1.2.3', 'tfmodel/v01.2.3', 'tfmodel/v1.2.3-SNAPSHOT', 'tfmodel/v1.2.3/x'):
            with self.assertRaises(ValueError):
                build.selection(value)
        self.assertEqual(build.command('tfmodel', '1.2.3')[2], ':tfmodel:publishToMavenLocal')
        self.assertNotIn(':carddetector:', ' '.join(build.command('tfmodel', '1.2.3')))

    def test_core_entrypoint_never_pulls_lfs_or_builds_sibling(self):
        with patch.dict(build.os.environ, {'VERSION': 'carddetector~v1.2.3', 'GIT_COMMIT': 'a'*40}), patch.object(build, 'run', return_value='a'*40), patch.object(build.subprocess, 'run') as execute:
            build.main()
        execute.assert_called_once_with(build.command('carddetector', '1.2.3'), cwd=build.ROOT, check=True)

    def test_model_entrypoint_scopes_lfs_before_build(self):
        with patch.dict(build.os.environ, {'VERSION': 'tfmodel~v1.2.3', 'GIT_COMMIT': 'a'*40}), patch.object(build, 'run', return_value='a'*40), patch.object(build.subprocess, 'run') as execute, patch.object(build, 'verify_models') as validate:
            build.main()
        self.assertEqual(execute.call_args_list[0].args[0], ['git', 'lfs', 'pull', '--include=' + build.MODEL_GLOB, '--exclude='])
        self.assertEqual(execute.call_args_list[1].args[0], build.command('tfmodel', '1.2.3'))
        validate.assert_called_once()

    def test_repository_coordinates_and_module_versions(self):
        for module, local_artifact in jp.ARTIFACTS.items():
            expected = (jp.GROUP, jp.ARTIFACT, f'{module}~v1.2.3')
            self.assertEqual(jp.coordinates(module, '1.2.3', local_artifact), expected)
            self.assertEqual(jp.coordinates(module, '1.2.3', jp.ARTIFACT), expected)
        with self.assertRaises(ValueError):
            jp.coordinates('tfmodel', '1.2.3', 'card-detector')
        record, _ = self.fixture()
        record['artifact'] = 'card-detector-model'
        with self.assertRaises(ValueError):
            jp.artifact_base(record)

    def fixture(self):
        record = dict(module='tfmodel', version='1.2.3', source='a'*40, group=jp.GROUP,
                      artifact=jp.ARTIFACT, consumer_version='tfmodel~v1.2.3',
                      core_version='0.1.0', core_artifact='core')
        asset = b'\x00'*4 + b'TFL3model'
        record['model_assets'] = {'assets/model.tflite': hashlib.sha256(asset).hexdigest()}
        metadata = '<name>n</name><description>d</description><url>u</url><licenses><license><name>l</name><url>u</url></license></licenses><developers><developer><id>i</id><name>n</name></developer></developers><scm><url>u</url><connection>c</connection></scm>'
        pom = f'<project><groupId>{jp.GROUP}</groupId><artifactId>{jp.ARTIFACT}</artifactId><version>tfmodel~v1.2.3</version><packaging>aar</packaging>{metadata}<dependencies><dependency><groupId>com.apexfission.android.carddetector</groupId><artifactId>core</artifactId><version>0.1.0</version><scope>compile</scope></dependency></dependencies></project>'.encode()
        module = {'component': {'group': jp.GROUP, 'module': record['artifact'], 'version': record['consumer_version']}, 'variants': [{'dependencies':[{'group': 'com.apexfission.android.carddetector', 'module':'core', 'version':{'requires':'0.1.0'}}]}]}
        aar = zipped({'AndroidManifest.xml': b'manifest', 'classes.jar': zipped({'com/apexfission/android/carddetector/tfmodel/Model.class': b'\xca\xfe\xba\xbe\x00\x00\x00\x3d'}), 'assets/model.tflite':asset})
        files = {jp.API_BASE + '/' + record['consumer_version']: json.dumps({'status':'ok','commit':record['source']}).encode()}
        base = jp.artifact_base(record)
        for suffix, data in {'.pom':pom, '.aar':aar, '-sources.jar':zipped({'Model.kt':'class Model'}), '-javadoc.jar':zipped({'README.md':'Model'}), '.module':json.dumps(module).encode()}.items():
            files[base + suffix] = data
        return record, files

    def test_verified_hashes_are_remote_and_metadata_optional(self):
        record, files = self.fixture()
        record['sha256'] = {'.aar':'intentionally-not-a-local-byte-match'}
        result = jp.verify(record, lambda url, missing=False: files.get(url))
        self.assertEqual(len(result), 5)
        self.assertEqual(result['.aar'], hashlib.sha256(files[jp.artifact_base(record)+'.aar']).hexdigest())
        del files[jp.artifact_base(record)+'.module']
        self.assertEqual(len(jp.verify(record, lambda url, missing=False:files.get(url))), 4)
        self.assertFalse(any(url.endswith('.asc') for url in files))

    def test_required_artifact_cannot_be_silently_omitted(self):
        record, files = self.fixture()
        del files[jp.artifact_base(record) + '-sources.jar']
        with self.assertRaises(jp.NotReady):
            jp.verify(record, lambda url, missing=False: files.get(url))

    def test_wrong_provenance_and_pin_fail(self):
        for mutation in ('commit', 'status', 'pin', 'metadata_pin', 'asset', 'coordinate'):
            record, files = self.fixture()
            base=jp.artifact_base(record)
            if mutation in ('commit', 'status'):
                provenance={'status':'ok','commit':record['source']}
                provenance[mutation]='bad'
                files[jp.API_BASE+'/'+record['consumer_version']]=json.dumps(provenance).encode()
            elif mutation == 'pin':
                files[base+'.pom']=files[base+'.pom'].replace(b'com.apexfission.android.carddetector',jp.GROUP.encode())
            elif mutation == 'metadata_pin':
                files[base+'.module']=files[base+'.module'].replace(b'0.1.0',b'9.9.9')
            elif mutation == 'asset':
                record['model_assets']['assets/model.tflite']='b'*64
            else:
                record['consumer_version']='tfmodel~v9.9.9'
            with self.subTest(mutation=mutation), self.assertRaises(ValueError):
                jp.verify(record,lambda url,missing=False:files.get(url))

    def test_pending_build_is_retryable_but_failed_build_is_not(self):
        for status in ('none', 'queued', 'pending', 'building', 'running', 'Building', 'error', 'Error', 'failed', 'unknown'):
            record, files = self.fixture()
            files[jp.API_BASE + '/' + record['consumer_version']] = json.dumps({'status': status}).encode()
            error = jp.NotReady if status.lower() in ('none', 'queued', 'pending', 'building', 'running') else ValueError
            with self.subTest(status=status), self.assertRaises(error):
                jp.verify(record, lambda url, missing=False: files.get(url))

    def test_lfs_pointer_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'model.tflite'
            path.write_text('version https://git-lfs.github.com/spec/v1\n')
            with patch.object(build,'ROOT',Path(directory)), patch.object(build,'run',return_value='model.tflite'):
                with self.assertRaisesRegex(ValueError,'LFS pointer'):
                    build.verify_models()


if __name__ == '__main__':
    unittest.main()
