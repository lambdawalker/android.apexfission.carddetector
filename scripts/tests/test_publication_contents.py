import io
import unittest
import zipfile
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parents[1]))
import module_release as m

class PublicationContentsTests(unittest.TestCase):
    def archive(self,name):
        data=io.BytesIO()
        with zipfile.ZipFile(data,'w') as z:
            z.writestr('README.md','Guide')
            z.writestr(name,'content')
        return data.getvalue()

    def test_text_documentation_and_manifest_allowed(self):
        for name in ['docs/guide.md','LICENSE','META-INF/MANIFEST.MF']:
            m.verify_documentation_contents(self.archive(name))

    def test_media_and_release_binaries_rejected(self):
        for name in ['docs/image.png','docs/image.svg','docs/demo.mp4','docs/demo.apk','docs/library.aar','docs/archive.zip','docs/weights.tflite','docs/program.bin']:
            with self.subTest(name=name),self.assertRaisesRegex(ValueError,'Unexpected documentation'):
                m.verify_documentation_contents(self.archive(name))
