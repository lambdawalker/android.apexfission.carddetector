# Project-specific helpers

Run `setup.bat` or `./setup.sh` at the repository root for the shared CLI.
Project-specific helpers are grouped by feature, with tests in each feature's
`tests/` directory.

| Folder | Responsibility |
| --- | --- |
| `documentation/` | `docs.py` checks Kotlin declarations and runnable snippets; `media.py` manages historical recording/frame provenance and integrity. |
| `screenshots/` | `screenshot_manifest.py` checks the scenarios in `screenshot-scenarios.json`; `verify_capture_ui_tests.py` checks required emulator test results. |
| `setup/` | PowerShell installers and launchers used by `setup.bat`, setup/JitPack bootstrap tests, and `requirements.txt` for the CI configuration comparison (PyYAML). |

The Python helpers and tests otherwise use the standard library. Run all tests
from the repository root:

```sh
python3 -m unittest discover -s scripts -p "test_*.py" -v
```

To run one feature's tests, use its directory, for example
`python3 -m unittest discover -s scripts/screenshots/tests -v`.

The Windows helpers support PowerShell 5.1. They read the project pin, verify
release checksums, cache the CLI under ignored `bin/`, and forward arguments and
exit status. JitPack installs the pinned CLI through `jitpack.yml` and invokes
`./setup.sh jitpack_build`.

Shared publishing, asset verification, installation documentation, release-history
export, and GitHub environment setup live in `android-repo-setup`. Historical
Python coordinators remain in Git history; recover an old attempt in a separate
checkout of its original source.
