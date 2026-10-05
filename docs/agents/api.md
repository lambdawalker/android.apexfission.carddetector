# Public API reference

These pages contain exact source-derived declaration excerpts, imports, and subsystem contracts. Function bodies are omitted in the reference; defaults are retained. The reviewed-input manifest fails the docs build when Kotlin inputs change. It is a documentation drift guard, not a binary compatibility checker.

| Subsystem | Reference |
| --- | --- |
| Live camera and video composables, presets, overlay scope | [Components](api/components.md) |
| Detection metadata, validators, closeable engines, model catalog | [Detection](api/detection.md) |
| Overlay functions, animation types, drawing helpers | [Overlays](api/overlays.md) |
| OCR, dHash, bitmap ownership helper | [Utilities](api/utilities.md) |
| Camera/video adapters and implementation-facing public ViewModels | [Advanced adapters](api/adapters.md) |

Kotlin `internal`/`private` declarations are excluded from consumer excerpts. Some public classes are implementation-facing, especially ViewModels, factories and animation state helpers; prefer high-level composables unless you deliberately own their lifecycle. YOLO and coordinates declarations are owned by those dependency repositories, not copied here.

Read [concepts](concepts.md) before calling these APIs. Core and model are independent artifacts; the model extensions require the model artifact. All examples describe main, while [installation](../../IMPORT.md) reports confirmed publication.
