import importlib.util
import struct
import tempfile
import unittest
import zlib
from pathlib import Path

spec = importlib.util.spec_from_file_location('screenshot_manifest', Path(__file__).parents[1] / 'screenshot_manifest.py')
manifest = importlib.util.module_from_spec(spec)
spec.loader.exec_module(manifest)


def png():
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 1, 1, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(b'\x00\x66\x66\x66')) + chunk(b'IEND', b''))


class ScreenshotManifestTests(unittest.TestCase):
    def image(self, root, scenario, suffix='hash'):
        path = root / 'package' / f'Fixture_{scenario}_{suffix}_0.png'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png())
        return path

    def test_requires_named_scenarios_not_just_image_count(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.image(root, 'wrong')
            with self.assertRaisesRegex(ValueError, 'Missing.*front'):
                manifest.collect_images(root, ['front'])

    def test_zero_and_duplicate_scenarios_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with self.assertRaisesRegex(ValueError, 'Missing'):
                manifest.collect_images(root, ['front'])
            self.image(root, 'front', 'first')
            self.image(root, 'front', 'second')
            with self.assertRaisesRegex(ValueError, 'Duplicate'):
                manifest.collect_images(root, ['front'])

    def test_records_exact_bytes_dimensions_and_scenario(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            path = self.image(root, 'front')
            record = manifest.collect_images(root, ['front'])
            self.assertEqual(record['scenarios']['front'], str(path.relative_to(root)))
            self.assertEqual(record['dimensions'][str(path.relative_to(root))], {'width': 1, 'height': 1})
            self.assertEqual(len(record['images'][str(path.relative_to(root))]), 64)

    def test_rejects_truncated_png(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.image(root, 'front').write_bytes(b'\x89PNG\r\n\x1a\n')
            with self.assertRaisesRegex(ValueError, 'Invalid PNG'):
                manifest.collect_images(root, ['front'])
