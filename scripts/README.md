# Project-specific helpers

Shared release coordination, artifact verification, installation documentation,
release-history export and GitHub environment setup live in `android-repo-setup`.
Run `setup.bat` or `./setup.sh` at the repository root.

Retained Python helpers:

- `docs.py`: Kotlin declaration inventory and runnable documentation snippets.
- `media.py`: provenance and integrity checks for the historical recording and frames.
- `screenshot_manifest.py`: project-specific preview scenarios and image manifests.
- `verify_capture_ui_tests.py`: required capture-guide emulator test inventory.

`install-setup-cli.ps1` and `run-setup-cli.ps1` support `setup.bat` (Windows
PowerShell 5.1). They read the project pin, verify downloaded release checksums,
cache the CLI under ignored `bin/`, and forward arguments and exit status.

`requirements-publishing.txt` contains only PyYAML, used by the CI inventory
comparison. Its existing filename is retained for generated workflow compatibility.
The retained Python helpers and their tests otherwise use the standard library.

Run `python3 -m unittest discover -s scripts/tests -v`. Old Python release
coordinators and their tests remain available in Git history; recover an old
attempt in a separate checkout of its original source, never against today's build.

JitPack tag selection and model asset preparation now use the pinned CLI through
`./setup.sh jitpack_build`; the bootstrap is in `jitpack.yml`.
