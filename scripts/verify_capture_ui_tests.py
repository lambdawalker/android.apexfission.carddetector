"""Fail if any required capture-guide/layout instrumentation case was filtered out."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

REQUIRED = {
    ('CaptureLayoutTest', 'landscapeTurnOverCanScrollToFullImageAndKeepActionsVisible'),
    ('CaptureReviewTest', 'largeFontSingleReviewKeepsRetakeReadable'),
    ('CaptureReviewTest', 'reviewExposesSeparateRetakesAndConfirmation'),
    ('CaptureReviewTest', 'singleReviewExposesOnlyFrontRetakeAndConfirmation'),
    ('CaptureReviewTest', 'turnOverWaitsForExplicitContinue'),
    ('CaptureOverlayTest', 'singleSideGuideFadesAndDoesNotReturnOnMiss'),
    ('CaptureOverlayTest', 'frontGuideFadesAndResetsForANewAttempt'),
    ('CaptureOverlayTest', 'backGuideFadesAndResetsForANewAttempt'),
    ('CaptureLayoutTest', 'narrowLongTextKeepsSingleSideToggleAndShutterReachable'),
    ('CaptureLayoutTest', 'largeFontTwoSideCaptureCanScrollToShutter'),
    ('CaptureLayoutTest', 'landscapeKeepsSingleSideShutterVisible'),
}


def verify(root):
    passed = set()
    for path in root.rglob('TEST-*.xml'):
        for case in ET.parse(path).getroot().iter('testcase'):
            key = (case.attrib.get('classname', '').rsplit('.', 1)[-1], case.attrib.get('name'))
            if key in REQUIRED:
                if any(case.find(tag) is not None for tag in ('failure', 'error', 'skipped')):
                    raise ValueError(f'Required UI test did not pass: {key}')
                passed.add(key)
    missing = REQUIRED - passed
    if missing:
        raise ValueError(f'Missing required UI tests: {sorted(missing)}')
    print(f'All {len(REQUIRED)} required capture UI tests passed')


if __name__ == '__main__':
    verify(Path(sys.argv[1]) if len(sys.argv) > 1 else
           Path('carddetector/build/outputs/androidTest-results/connected/debug'))
