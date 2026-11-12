# Project-specific documentation tools

Only two executable helpers remain:

- `docs.py`: reviewed Kotlin declaration excerpts and runnable examples, including Card Detector-specific prose and classification.
- `media.py`: historical recording/frame rendering and acceptance policy.

Bootstrap, setup drift checks, screenshot manifests, and required JUnit checks are owned by the shared CLI. Project-specific inputs live in `.project/verification/checks.json`.

Run `python3 -m unittest discover -s docs/tests -v` for documentation extractor tests. Bootstrap and shared-check tests live in the CLI repository.
