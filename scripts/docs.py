#!/usr/bin/env python3
"""Generate reviewed Kotlin declaration excerpts and verify documentation input hashes.

This small source extractor is a documentation aid, not a Kotlin compiler/ABI checker.
The manifest intentionally hashes complete source files so changes require human review.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / 'docs/api-inputs.json'
DECL = re.compile(r'(?m)^[ \t]*(?:(?:companion|public|private|internal|protected|override|open|abstract|inline|suspend|data|sealed|enum|fun|const|lateinit|final|tailrec|operator|infix)\s+)*(?:class|interface|object|fun|val|var|typealias|constructor)\b')


def masked(text):
    # Preserve offsets/newlines while ignoring braces in comments and string literals.
    pattern = r'/\*[\s\S]*?\*/|//[^\n]*|"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\''
    return re.sub(pattern, lambda m: ''.join('\n' if c == '\n' else ' ' for c in m[0]), text)


def end_brace(mask, start):
    depth = 1
    for i in range(start + 1, len(mask)):
        depth += (mask[i] == '{') - (mask[i] == '}')
        if depth == 0:
            return i
    raise ValueError('Unbalanced Kotlin block')


def declarations(text):
    mask = masked(text)
    def section(lo, hi, indent=''):
        out, cursor = [], lo
        while True:
            match = DECL.search(mask, cursor, hi)
            if not match:
                break
            start = match.start()
            # Only declarations at this lexical scope; bodies are skipped below.
            prefix = mask[start:match.end()]
            hidden = bool(re.search(r'\b(private|internal|protected)\b', prefix))
            is_type = bool(re.search(r'\b(class|interface|object)\b', prefix))
            is_property = bool(re.search(r'\b(val|var|typealias)\b', prefix))
            depth, i = 0, match.end()
            while i < hi:
                c = mask[i]
                if c in '([': depth += 1
                elif c in ')]': depth -= 1
                if depth == 0 and (c in '{=\n'):
                    break
                i += 1
            header = text[start:i].strip()
            annotations = list(re.finditer(r'(?m)^([ \t]*@Composable)[ \t]*$', text[lo:start]))
            annotation = annotations[-1] if annotations else None
            if annotation and not text[lo+annotation.end():start].strip():
                header = '@Composable\n' + header
            if i < hi and mask[i] == '{':
                end = end_brace(mask, i)
                if not hidden:
                    if is_type:
                        if re.search(r'\benum\s+class\b', prefix):
                            out.append(indent + text[start:end+1].strip())
                        else:
                            body = section(i+1, end, indent+'    ')
                            out.append(indent + header + ' {\n' + body + '\n' + indent + '}')
                    else:
                        out.append(indent + header + ' /* body omitted */')
                cursor = end + 1
            elif i < hi and mask[i] == '=':
                # Skip expression implementation, including multi-line constructor calls.
                j, nesting = i+1, 0
                while j < hi:
                    c = mask[j]
                    if c in '([{': nesting += 1
                    elif c in ')]}': nesting -= 1
                    if c == '\n' and nesting <= 0 and text[i+1:j].strip(): break
                    j += 1
                if not hidden:
                    if is_property:
                        # Keep exact default/preset values in references.
                        out.append(indent + text[start:j].strip())
                    else:
                        out.append(indent + header + ' /* expression body omitted */')
                cursor = j+1
            else:
                if not hidden:
                    out.append(indent + header)
                cursor = i+1
        return '\n\n'.join(out)
    return section(0, len(text))


def category(path):
    if '/ui/overlays/' in path: return 'overlays'
    if '/domain/ocr/' in path or '/domain/tflite/image/' in path or '/resource/' in path: return 'utilities'
    if any(path.endswith('/'+n+'.kt') for n in ['CardDetectorLite','CardTrackingSimulator','CardDetectorPreset','CameraPreset','CardDetectorOverlayScope']): return 'components'
    if '/ui/' in path: return 'adapters'
    return 'detection'


INTROS = {
 'components': '''# Camera and video components

Call composables from a lifecycle-aware Compose host. Obtain camera permission first. `instanceKey` must be nonblank, stable, and unique among sibling detectors. Models/classes/validators configure the detector; presets control admission and tracking. See [configuration](../configuration.md) for parameter meanings and actual preset values.

`CardDetectorLite` returns Unit and wires CameraX to worker detection; its camera default is HighResolution. Both callback lambdas receive a host-owned cutout; always consume/recycle them. `onBack` is an overlay user action. The default overlay is empty. Simulator uses a readable `videoUri`, requires `onCardDetection`, and names its actions `onCaptureRequested` / `onBackRequested`. It has no torch. See [ownership and lifetime](../concepts.md).

Scope `detectionState`/`cardDetection` describe the current evaluation, `detectionSequence` changes on misses too, and `latestBestDetection`/`captureEnabled` refer to retained capture. `capture()` copies retained data, `goBack()` invokes the host action, and `toggleFlashlight()` requests torch state where available. `imageSpaceChain` can be null before preview measurement.
''',
 'detection': '''# Detection, models, and validators

`CardDetection` is metadata plus tracking state. Its `card` and `features` are YOLO `Feature` objects, not images. IDs are nullable while locking; `DetectionSource` distinguishes inference from hash continuation. The callback bitmap is delivered separately. Source-frame coordinate contracts are in [concepts](../concepts.md).

`CardDetector.track` is synchronous and returns null on no candidate. Bitmap/proxy inputs remain caller-owned; close proxies after the call. Close the detector to close its YOLO dependency. Both builders use the same defaults and a thread-confined wrapper. An optional shared physical dispatcher is borrowed and must outlive borrowers. After the wrapper is closed, track returns null and enabled reads false. Do not mutate/close the input while a synchronous call is active. See [recipes](../recipes.md).

Validators receive borrowed frames and return Boolean. Margin defaults to 20 pixels; aspect ratio accepts longest/shortest within 1.28..1.7 inclusive and rejects zero dimensions. The `configurationKey` controls recomposition identity. Primary card classes must be nonempty. Keep thresholds in sensible ranges; builder construction is not a comprehensive user-input validator.

The `ModelCatalog` marker belongs to core; tfmodel extension properties require the optional model artifact and explicit imports. The legacy carddetector `PreProcessingImageTransformation` remains exported, but presets require the distinct YOLO type. Do not interchange them.
''',
 'overlays': '''# Overlays, animation, and drawing

Use scoped composables inside `controlOverlay`. `IdCaptureOverlay` combines indicator and controls; the split functions let the host place them independently. Capture calls the retained-image action; changing UI gating does not implement fresh capture. `DetectionOverlay` renders metadata through the image-space chain; `DebugOverlay` renders diagnostics. Unscoped overloads need explicit data. Missing mapping/detection can suppress drawing.

`IdCaptureOverlayConfig` holds text, color, timing, smoothing, opacity and gating defaults; meanings are in [configuration](../configuration.md). Prefer canonical types from `ui.overlays.animation`; older overlay-package aliases/wrappers remain for compatibility. `AnimatedDetectionBounds` offers both structured and coordinate constructors and convenience getters. Bounds are screen-space animation output, not a bitmap.

`AnimatedDetectionCanvas` provides a draw scope plus animated bounds. Pure path/segment builders construct rounded geometry; DrawScope helpers paint it without owning image resources. Call drawing functions only during drawing and state helpers only in composition. `shouldRunContinuousAnimations` is a policy helper, not a detector. `InternalGuideState` is publicly declared despite its name; most hosts should use the high-level overlays. Keep animation dimensions/timings finite and nonnegative; no universal validation is promised.
''',
 'utilities': '''# Bitmap ownership, perceptual hashes, and OCR

`Bitmap.use` recycles in finally and propagates the block result/error. Do not use it around work that escapes the block. It transfers no ownership to a launched coroutine by itself.

The dHash helpers are synchronous CPU work. Input images remain caller-owned; temporary scaled images are released. `hashSize` must be 2..8 or `IllegalArgumentException` is thrown. Region hashing clamps bounds to the bitmap and rejects an empty clamped region. Hamming distance returns differing bit count; both `isVisuallySimilar` overloads use an inclusive threshold. Similarity is not identity, authenticity, or a collision-resistant hash.

`OcrWrapper()` owns an ML Kit Latin recognizer. `run(Bitmap)` suspends and returns text blocks with nullable Rect bounds relative to its input. It neither recycles the bitmap nor parses IDs/barcodes. ML Kit failures propagate; new calls after close throw IllegalStateException. Close is idempotent and disposes the recognizer once active wrapper requests finish. Keep the input valid through processing; see the ownership recipe.
''',
 'adapters': '''# Advanced camera and video adapters

These public declarations expose lower-level integration points. Prefer `CardDetectorLite` / `CardTrackingSimulator`: custom adapters must reproduce ownership, lifecycle, dispatching, and cleanup themselves. Public ViewModels and factories are implementation-facing; their signatures do not imply safe independent native-resource sharing.

`CameraPreview` binds preview and analysis to the supplied lifecycle owner. Its `onFrame` receiver must close every ImageProxy. Frame callbacks follow CameraX dispatch, not a main-thread UI guarantee. `createPreviewImageSpaceChain` constructs rotation/preview mapping; use actual measured sizes, not guessed screen dimensions. `AutoFocusPolicy` is stateful; serialize access, pass a consistent clock, and reset for a new session. It only recommends a point/action.

`VideoPreviewWithFullFrameCapture` owns/releases its Media3 player; its frame consumer owns delivered bitmaps. `BitmapFrameProcessor` invokes callbacks on the supplied executor, transfers ownership at delivery, and clamps capture intervals. `release()` is currently a no-op; queued callbacks can outlive it. These APIs use Media3 unstable interfaces and may need OptIn in host code.

ViewModel process methods admit work with a single-flight gate; rejected frames are released. `captureLatest` delivers an independent owned copy on a worker. `setDetectionEnabled` controls processing; it does not cancel already-delivered host tasks. Native resources and retained/callback buffers are released by ViewModel clearing. Factories are intended for the Android ViewModel system, not repeated manual construction. All callbacks and retained-image caveats in [concepts](../concepts.md) apply.
''',
}


def expected():
    records, pages = [], {k: v+'\n## Declarations\n\nExact source excerpts; implementation bodies are omitted. Imports below provide type resolution, including some implementation-only imports.\n' for k,v in INTROS.items()}
    for module in ['carddetector','tfmodel']:
        for p in sorted((ROOT/module/'src/main/java').rglob('*.kt')):
            text = p.read_text(); rel = p.relative_to(ROOT).as_posix(); cat = category(rel)
            decl = declarations(text)
            records.append({'path':rel,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'guide':f'docs/agents/api/{cat}.md' if decl else None,'visibility':'public' if decl else 'internal/private'})
            if not decl: continue
            package = re.search(r'^package .+$',text,re.M)[0]
            imports = '\n'.join(re.findall(r'^import .+$',text,re.M))
            pages[cat] += f'\n### {p.stem} ({module})\n\n[Source](../../../{rel})\n\n```kotlin\n{package}\n\n{imports}\n\n{decl}\n```\n'
    return records, pages


def main():
    parser = argparse.ArgumentParser();parser.add_argument('action',choices=['update','verify']);args=parser.parse_args()
    records,pages=expected(); outputs={MANIFEST:json.dumps(records,indent=2)+'\n'}
    outputs.update({ROOT/f'docs/agents/api/{k}.md':v for k,v in pages.items()})
    # Exact runnable examples, expanded in committed Markdown and checked in CI.
    for guide,name,path in [('quickstart','quickstart','DocumentationQuickstartActivity.kt'),('recipes','ownership','OwnedBitmapWork.kt')]:
        p=ROOT/f'docs/agents/{guide}.md'; old=p.read_text()
        start=f'<!-- example: {name} -->';end=f'<!-- end-example: {name} -->'
        source=(ROOT/'app/src/main/java/com/apexfission/android/carddetector/demo'/path).read_text().strip()
        fresh=old[:old.index(start)]+start+'\n\n```kotlin\n'+source+'\n```\n\n'+old[old.index(end):]
        outputs[p]=fresh
    failed=[]
    for path,text in outputs.items():
        if args.action=='update': path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
        elif not path.exists() or path.read_text()!=text: failed.append(str(path.relative_to(ROOT)))
    if failed: raise SystemExit('Review source/docs and run python3 scripts/docs.py update: '+', '.join(failed))
    print(f'{args.action}: {len(records)} Kotlin inputs classified; public excerpts and runnable snippets synchronized.')

if __name__=='__main__': main()
