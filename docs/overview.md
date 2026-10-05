# Detect. Track. Capture.

Build an Android card-scanning experience with on-device detection, temporal tracking, and cropped images for your own processing pipeline. CameraX and Jetpack Compose handle the preview; the detector follows a primary card and exposes capture controls through an overlay slot.

**[Start integrating](agents/quickstart.md)** · **[Install](../IMPORT.md)** · **[Watch and run the demos](agents/demos.md)** · **[AI documentation](agents/index.md)**

<!-- recording-player -->

## Visual description

The supplied portrait recording shows the scanning screen, card guide, and shutter. The guide follows a sample card in the preview. This September 2026 recording illustrates the interaction; it is not a measurement of accuracy or current-release behavior. [Details and runnable demos](agents/demos.md).

## Choose your integration

| Need | Start with | Host responsibility |
| --- | --- | --- |
| Live camera | `CardDetectorLite` | Runtime permission, session lifecycle, callback images |
| Repeatable input video | `CardTrackingSimulator` | Readable URI, playback/device verification |
| Your own UI | `CardDetectorOverlayScope` | Layout, actions, app-specific copy |
| Your own model | `core` plus compatible model assets | Tensor contract, labels, card classes |
| Included Sentinel model | `sentinel-card-model` | Import model catalog extensions |
| Pipeline without Compose | `buildCardDetector` | Input ownership, threading, cleanup |

## What comes out

A detection contains the primary card box, secondary feature metadata, tracking ID/progress, and source (`Yolo` or `Hash`). Compose callbacks also deliver an independently owned card-cutout bitmap. Explicit capture copies the retained best result. It does not request a new camera still.

Detection and tracking are the first stage of a document pipeline. They do not authenticate documents, decode barcodes, verify people, or provide a complete OCR workflow. Optional OCR returns text blocks. The host decides what to process, where to send it, and when to discard it.

## A complete path from install to capture

The [quickstart](agents/quickstart.md) includes dependency guidance, manifest, imports, a complete demo Activity, permission recovery, and bitmap cleanup. Then read [configuration](agents/configuration.md), [lifecycle and ownership](agents/concepts.md), [task recipes](agents/recipes.md), and the [public API reference](agents/api.md).

This website describes main development source; its banner identifies the exact build. [Installation](../IMPORT.md) comes from confirmed release metadata. The [maintenance guide](maintenance.md) explains how both documentation surfaces stay aligned.
