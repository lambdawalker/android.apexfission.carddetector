# Two-sided capture

Approved direction: editable Figma concept https://www.figma.com/design/o1TcglRUCJPb5pavGwpxPu?node-id=2-2 and user's instruction to implement.

Add an opt-in two-sided Compose capture component around CardDetectorLite. Show front/back instructions on the feed and 1/2 or 2/2 above the shutter. Use a square aiming area and adaptive document outline, with rectangular crops. Pause after the front; Continue to back starts a fresh capture attempt. Review both images and allow independent retakes before delivering an owned pair to the host. English defaults and Spanish Android resources follow locale and support caller overrides.

Auto-capture defaults off. It requires fresh, focused, stable detections across time; manual shutter remains available. Report card-region sharpness as focus metadata, separate from tracking confidence and camera autofocus success. Prefer focused retained images. Do not claim front/back classification or authenticity. Preserve existing single-side call sites.

Each attempt owns an isolated detector ViewModelStore, disposed between attempts, preventing old retained images and tracking from crossing sides. The flow owns undelivered images; the host owns the pair only on confirmation. Dispose, cancellation and retake must release abandoned images. Configuration recreation restarts capture rather than serializing bitmaps.

Alternatives considered: morph a square into the document (less clutter, moving target); corner brackets only (less initial guidance). Selected square plus outline as presented. Explicit turn-over transition is the implementation default.
