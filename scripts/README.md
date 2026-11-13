# Shared repository tools

Documentation source extraction, examples, media rendering and acceptance now belong to `android-repo-docs`. Use the root `docs.sh` or `docs.bat` launcher. Project mappings and introductory prose live in `.project/docs/`; runtime helpers are no longer copied into this folder.

Setup, publishing, required JUnit validation, and GitHub configuration remain owned by `android-repo-setup`, through `setup.sh` / `setup.bat` and `.project/setup/`. Shared tool regression tests live in `gradle.android-repo-setup`.
