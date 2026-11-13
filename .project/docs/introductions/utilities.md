# Bitmap ownership, perceptual hashes, and OCR

`Bitmap.use` recycles in finally and propagates the block result/error. Do not use it around work that escapes the block. It transfers no ownership to a launched coroutine by itself.

The dHash helpers are synchronous CPU work. Input images remain caller-owned; temporary scaled images are released. `hashSize` must be 2..8 or `IllegalArgumentException` is thrown. Region hashing clamps bounds to the bitmap and rejects an empty clamped region. Hamming distance returns differing bit count; both `isVisuallySimilar` overloads use an inclusive threshold. Similarity is not identity, authenticity, or a collision-resistant hash.

`OcrWrapper()` owns an ML Kit Latin recognizer. `run(Bitmap)` suspends and returns text blocks with nullable Rect bounds relative to its input. It neither recycles the bitmap nor parses IDs/barcodes. ML Kit failures propagate; new calls after close throw IllegalStateException. Close is idempotent and disposes the recognizer once active wrapper requests finish. Keep the input valid through processing; see the ownership recipe.
