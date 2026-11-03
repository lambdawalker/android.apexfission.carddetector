import importlib.util
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('capture_ui_inventory', Path(__file__).parents[1] / 'verify_capture_ui_tests.py')
inventory = importlib.util.module_from_spec(spec)
spec.loader.exec_module(inventory)


class CaptureUiInventoryTests(unittest.TestCase):
    def report(self, root, cases, skipped=False):
        suite = ET.Element('testsuite')
        for cls, name in cases:
            case = ET.SubElement(suite, 'testcase', classname='package.' + cls, name=name)
            if skipped:
                ET.SubElement(case, 'skipped')
        ET.ElementTree(suite).write(root / 'TEST-device.xml')

    def test_requires_layout_cases_even_when_guide_cases_pass(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.report(root, [case for case in inventory.REQUIRED if case[0] == 'CaptureOverlayTest'])
            with self.assertRaisesRegex(ValueError, 'CaptureLayoutTest'):
                inventory.verify(root)

    def test_accepts_all_six_passed_cases(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.report(root, inventory.REQUIRED)
            inventory.verify(root)

    def test_skipped_cases_do_not_count_as_passed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.report(root, inventory.REQUIRED, skipped=True)
            with self.assertRaisesRegex(ValueError, 'did not pass'):
                inventory.verify(root)
