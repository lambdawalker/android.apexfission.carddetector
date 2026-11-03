"""Validate the named screenshot inventory and record candidate provenance (no acceptance)."""
import argparse
import hashlib
import json
import struct
import subprocess
import tomllib
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[1]


def collect_images(root, expected):
    images = sorted(root.rglob('*.png'))
    scenarios, hashes, dimensions = {}, {}, {}
    for scenario in expected:
        matches = [path for path in images if f'_{scenario}_' in path.name]
        if not matches:
            raise ValueError(f'Missing required screenshot: {scenario}')
        if len(matches) != 1:
            raise ValueError(f'Duplicate screenshot scenario: {scenario}')
        scenarios[scenario] = str(matches[0].relative_to(root))
    if len(set(scenarios.values())) != len(images) or len(expected) != len(set(expected)):
        raise ValueError('Unexpected images, ambiguous filenames, or duplicate inventory entries')
    for path in images:
        data = path.read_bytes()
        if len(data) < 33 or data[:8] != b'\x89PNG\r\n\x1a\n' or data[12:16] != b'IHDR':
            raise ValueError(f'Invalid PNG: {path}')
        width, height = struct.unpack('>II', data[16:24])
        if not width or not height:
            raise ValueError(f'Invalid PNG dimensions: {path}')
        name = str(path.relative_to(root))
        hashes[name] = hashlib.sha256(data).hexdigest()
        dimensions[name] = {'width': width, 'height': height}
    return {'scenarios': scenarios, 'images': hashes, 'dimensions': dimensions}


def main():
    inventory = json.loads((REPOSITORY / 'scripts/screenshot-scenarios.json').read_text())
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--module', required=True, choices=inventory)
    args = parser.parse_args()
    root = REPOSITORY / args.module / 'src/screenshotTestDebug/reference'
    record = collect_images(root, inventory[args.module])
    versions = tomllib.loads((REPOSITORY / 'gradle/libs.versions.toml').read_text())['versions']
    record.update({
        'schema': 1,
        'source': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPOSITORY, text=True).strip(),
        'module': args.module,
        'renderer': 'Compose Preview Screenshot Testing ' + versions['screenshotValidation'],
        'kind': 'host-rendered synthetic UI; no camera or model execution',
        'status': 'candidates; second-render comparison verifies repeatability, not approved regression baselines',
        'camera_background': 'production checkerboardBackground modifier in camera overlay fixtures',
    })
    (root / 'render-manifest.json').write_text(json.dumps(record, indent=2) + '\n')
    print(f'Validated {len(record["images"])} named screenshots for {args.module}')


if __name__ == '__main__':
    main()
