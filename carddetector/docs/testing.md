# Testing the core library

[Documentation index](README.md) · [Test package map](packages.md)

Run commands from the repository root using the checked-in Gradle wrapper. Android tasks need the configured Android SDK (compileSdk 36), Java toolchain 17, and resolvable dependencies.

```bash
./gradlew :carddetector:testDebugUnitTest
./gradlew :carddetector:assembleDebugAndroidTest
./gradlew :carddetector:connectedDebugAndroidTest
```

The first task runs local tests of tensor validation, buffers, tracking, geometry safety, callback ownership, lifecycle policy, and UI configuration. Mocks/stubs do not demonstrate actual model accuracy or device GPU behavior.

The second only builds the instrumented test APK. The third runs real Android tests on a connected device/emulator, including image/video fixtures and interpreter lifecycle/output ownership. GPU coverage depends on device support.

## Diagnostic images

```bash
./gradlew :carddetector:runTestsAndExtractImages -PimageTestsOnly
```

This custom task depends on connectedDebugAndroidTest. The property filters instrumented tests by the GenerateImage annotation. PNG output is copied into `carddetector/test/results/detection`; the task also attempts cleanup of the device test-output directory. These images are diagnostics, not a replacement for assertions.

When changing coordinate handling or model assets, inspect detection overlays against fixture expectations on a device. When changing lifecycle code, include the focused thread/ownership tests identified in the package map.
