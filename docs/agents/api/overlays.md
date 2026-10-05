# Overlays, animation, and drawing

Use scoped composables inside `controlOverlay`. `IdCaptureOverlay` combines indicator and controls; the split functions let the host place them independently. Capture calls the retained-image action; changing UI gating does not implement fresh capture. `DetectionOverlay` renders metadata through the image-space chain; `DebugOverlay` renders diagnostics. Unscoped overloads need explicit data. Missing mapping/detection can suppress drawing.

`IdCaptureOverlayConfig` holds text, color, timing, smoothing, opacity and gating defaults; meanings are in [configuration](../configuration.md). Prefer canonical types from `ui.overlays.animation`; older overlay-package aliases/wrappers remain for compatibility. `AnimatedDetectionBounds` offers both structured and coordinate constructors and convenience getters. Bounds are screen-space animation output, not a bitmap.

`AnimatedDetectionCanvas` provides a draw scope plus animated bounds. Pure path/segment builders construct rounded geometry; DrawScope helpers paint it without owning image resources. Call drawing functions only during drawing and state helpers only in composition. `shouldRunContinuousAnimations` is a policy helper, not a detector. `InternalGuideState` is publicly declared despite its name; most hosts should use the high-level overlays. Keep animation dimensions/timings finite and nonnegative; no universal validation is promised.

## Declarations

Exact source excerpts; implementation bodies are omitted. Imports below provide type resolution, including some implementation-only imports.

### AnimatedDetectionBounds (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/AnimatedDetectionBounds.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays

import com.apexfission.android.carddetector.ui.overlays.animation.AnimatedDetectionBounds as AnimationAnimatedDetectionBounds
import com.apexfission.android.carddetector.ui.overlays.animation.DetectionAnimationConfig as AnimationDetectionAnimationConfig

typealias AnimatedDetectionBounds = AnimationAnimatedDetectionBounds

typealias DetectionAnimationConfig = AnimationDetectionAnimationConfig
```

### AnimatedDetectionCanvas (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/AnimatedDetectionCanvas.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.overlays.animation.AnimatedDetectionCanvas as AnimationAnimatedDetectionCanvas
import com.apexfission.android.carddetector.ui.overlays.animation.AnimatedDetectionScope as AnimationAnimatedDetectionScope
import com.apexfission.android.carddetector.ui.overlays.animation.state.shouldRunContinuousAnimations as animationShouldRunContinuousAnimations

typealias AnimatedDetectionScope = AnimationAnimatedDetectionScope

@Composable
fun CardDetectorOverlayScope.AnimatedDetectionCanvas(
    modifier: Modifier = Modifier,
    config: DetectionAnimationConfig = DetectionAnimationConfig(),
    onDraw: AnimatedDetectionScope.() -> Unit
) /* body omitted */

fun shouldRunContinuousAnimations(
    isTracking: Boolean,
    opacity: Float,
    motionEnabled: Boolean,
    hasVisibleBounds: Boolean
): Boolean /* expression body omitted */
```

### CardLockOnOverlay (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/CardLockOnOverlay.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.overlays.draw.buildConnectors
import com.apexfission.android.carddetector.ui.overlays.draw.buildCorners
import com.apexfission.android.carddetector.ui.overlays.draw.drawGlowPath
import com.apexfission.android.carddetector.ui.overlays.draw.lerpF
import kotlin.math.min

@Composable
fun CardDetectorOverlayScope.CardLockOnOverlay(
    config: DetectionAnimationConfig = DetectionAnimationConfig(
        idleOpacity = 0f,
        detectedOpacity = 1f,
        resetPositionOnMissing = false,
        fadeAnimationDurationMs = 300
    )
) /* body omitted */
```

### DebugOverlay (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/DebugOverlay.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.yolo.image.PreProcessingImageTransformation
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.yolo.engine.NumThreads

@Composable
fun CardDetectorOverlayScope.DebugOverlay(
    modifier: Modifier = Modifier,
    isDetectionEnabled: Boolean = true,
    showBoundingBoxes: Boolean = false,
    showLockOnProgress: Boolean = true
) /* body omitted */

@Composable
fun DebugOverlay(
    isDetectionEnabled: Boolean,
    useGpu: Boolean,
    showBoundingBoxes: Boolean,
    showLockOnProgress: Boolean,
    imageMode: PreProcessingImageTransformation,
    inferenceIntervalMs: Long,
    tapToFocusEnabled: Boolean,
    focusOnCardEnabled: Boolean,
    lockOnThreshold: Int,
    numThreads: NumThreads,
    modifier: Modifier = Modifier,
    noDetectionCountLimit: Int = 8,
    memoryDetectionTimeLimit: Long = 1000L,
    validateClassIdInLockOnProcess: Boolean = true,
    differenceHashDistanceLimit: Int = 25,
    allowTemporalDrift: Boolean = true,
) /* body omitted */
```

### DetectionOverlay (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/DetectionOverlay.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.math.transformations.translate
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope

@Composable
fun CardDetectorOverlayScope.DetectionOverlay(
    showClassNames: Boolean = false,
    classLabels: Map<Int, String> = this.classLabels
) /* body omitted */

@Composable
fun DetectionOverlay(
    cardDetection: CardDetection?, imageSpaceChain: ImageSpaceChain, showClassNames: Boolean, classLabels: Map<Int, String>
) /* body omitted */
```

### IdCaptureOverlay (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/IdCaptureOverlay.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.overlays.draw.lerpF

@Composable
fun CardDetectorOverlayScope.IdCaptureIndicatorOverlay(
    config: IdCaptureOverlayConfig = IdCaptureOverlayConfig()
) /* body omitted */

@Composable
fun CardDetectorOverlayScope.IdCaptureControlsOverlay(
    config: IdCaptureOverlayConfig = IdCaptureOverlayConfig()
) /* body omitted */

@Composable
fun CardDetectorOverlayScope.IdCaptureOverlay(
    config: IdCaptureOverlayConfig = IdCaptureOverlayConfig()
) /* body omitted */
```

### IdCaptureOverlayConfig (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/IdCaptureOverlayConfig.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

data class IdCaptureOverlayConfig(
    val idleOpacity: Float = 0.35f,
    val detectedOpacity: Float = 0.85f,
    val resetBoundingBoxWaitTimeMs: Long = 1_000L,
    val trackingAnimationDurationMs: Int = 200,
    val resetAnimationDurationMs: Int = 300,
    val fadeAnimationDurationMs: Int = 1_000,
    val enableGuideSmoothing: Boolean = true,
    val guideSmoothingFactor: Float = 0.2f,
    val requiresCardDetectionForCapture: Boolean = true,
    val title: String = "Verify Your Identity",
    val instructionTitle: String = "Position your ID within the frame",
    val instructionSubTitle: String = "We'll use this to pre-fill your information securely",
    val guideColor: Color = Color(0xFF2979FF),
    val enableContinuousAnimations: Boolean = false,
)
```

### AnimatedDetectionBounds (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/animation/AnimatedDetectionBounds.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays.animation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.carddetector.domain.tflite.model.CardDetection

typealias BoundingBoxCoordinates = ImageBox

val ImageBox.center: Offset get() = Offset(left + intWidth / 2f, top + intHeight / 2f)

val ImageBox.topLeft: Offset get() = Offset(left.toFloat(), top.toFloat())

val ImageBox.size: Size get() = Size(intWidth.toFloat(), intHeight.toFloat())

data class LockOnProgressState(
    val lockOnProgress: Float = 0f, val smoothProgress: Float = lockOnProgress
)

data class AnimationEffectsState(
    val breathe: Float = 1f, val sweepPhase: Float = 0f, val opacity: Float = 1f
)

data class TrackingMetadata(
    val isTracking: Boolean = false, val activeDetection: CardDetection? = null
)

data class AnimatedDetectionBounds(
    val coordinates: BoundingBoxCoordinates,
    val progress: LockOnProgressState = LockOnProgressState(),
    val effects: AnimationEffectsState = AnimationEffectsState(),
    val tracking: TrackingMetadata = TrackingMetadata()
) {
    constructor(
        left: Number,
        top: Number,
        right: Number,
        bottom: Number,
        lockOnProgress: Float = 0f,
        smoothProgress: Float = lockOnProgress,
        breathe: Float = 1f,
        sweepPhase: Float = 0f,
        opacity: Float = 1f,
        isTracking: Boolean = false,
        activeDetection: CardDetection? = null
    ) : this(
        coordinates = BoundingBoxCoordinates.from2P(left.toInt(), top.toInt(), right.toInt(), bottom.toInt()),
        progress = LockOnProgressState(lockOnProgress, smoothProgress),
        effects = AnimationEffectsState(breathe, sweepPhase, opacity),
        tracking = TrackingMetadata(isTracking, activeDetection)
    )

    val left: Float get() = coordinates.left.toFloat()

    val top: Float get() = coordinates.top.toFloat()

    val right: Float get() = coordinates.right.toFloat()

    val bottom: Float get() = coordinates.bottom.toFloat()

    val lockOnProgress: Float get() = progress.lockOnProgress

    val smoothProgress: Float get() = progress.smoothProgress

    val breathe: Float get() = effects.breathe

    val sweepPhase: Float get() = effects.sweepPhase

    val opacity: Float get() = effects.opacity

    val isTracking: Boolean get() = tracking.isTracking

    val activeDetection: CardDetection? get() = tracking.activeDetection

    val width: Float get() = coordinates.width.toFloat()

    val height: Float get() = coordinates.height.toFloat()

    val center: Offset get() = coordinates.center

    val topLeft: Offset get() = coordinates.topLeft

    val size: Size get() = coordinates.size
}

data class DetectionAnimationConfig(
    val idleOpacity: Float = 0.35f,
    val detectedOpacity: Float = 0.85f,
    val trackingAnimationDurationMs: Int = 200,
    val resetAnimationDurationMs: Int = 300,
    val fadeAnimationDurationMs: Int = 1_000,
    val resetDetectionIndicatorTime: Long = 150,
    val enableGuideSmoothing: Boolean = true,
    val resetPositionOnMissing: Boolean = true,
    val enableContinuousAnimations: Boolean = true,
)
```

### AnimatedDetectionCanvas (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/animation/AnimatedDetectionCanvas.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.overlays.animation.state.rememberAnimatedDetectionBounds

class AnimatedDetectionScope(
    private val drawScope: DrawScope,
    val bounds: AnimatedDetectionBounds
) : DrawScope by drawScope

@Composable
fun CardDetectorOverlayScope.AnimatedDetectionCanvas(
    modifier: Modifier = Modifier.fillMaxSize(),
    config: DetectionAnimationConfig = DetectionAnimationConfig(),
    onDraw: AnimatedDetectionScope.() -> Unit
) /* body omitted */
```

### InternalGuideState (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/animation/state/InternalGuideState.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays.animation.state



enum class InternalGuideState {
    IDLE, LOCKING, LOCKED
}
```

### RememberAnimatedDetectionBounds (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/animation/state/RememberAnimatedDetectionBounds.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays.animation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.carddetector.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetector.ui.overlays.animation.AnimatedDetectionBounds
import com.apexfission.android.carddetector.ui.overlays.animation.AnimationEffectsState
import com.apexfission.android.carddetector.ui.overlays.animation.DetectionAnimationConfig
import com.apexfission.android.carddetector.ui.overlays.animation.LockOnProgressState
import com.apexfission.android.carddetector.ui.overlays.animation.TrackingMetadata

@Composable
fun CardDetectorOverlayScope.rememberAnimatedDetectionBounds(
    defaultBox: ImageBox? = null,
    config: DetectionAnimationConfig = DetectionAnimationConfig()
): AnimatedDetectionBounds /* body omitted */
```

### rememberAnimatedOpacity (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/animation/state/rememberAnimatedOpacity.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays.animation.state

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.apexfission.android.carddetector.ui.overlays.animation.DetectionAnimationConfig

@Composable
fun rememberAnimatedOpacity(
    internalGuideState: InternalGuideState,
    config: DetectionAnimationConfig
): Float /* body omitted */
```

### PathUtils (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/draw/PathUtils.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays.draw

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.hypot
import kotlin.math.min

fun DrawScope.drawBlurredPath(
    points: List<Pair<Float, Float>>,
    blurRadius: Float,
    color: Color,
    strokeWidth: Float,
    blurStyle: BlurMaskFilter.Blur = BlurMaskFilter.Blur.NORMAL,
    rounded: Boolean = false,
    cornerRadius: Float = 0f
) /* body omitted */

fun DrawScope.drawGlowPath(
    points: List<Pair<Float, Float>>,
    blurRadius: Float,
    color: Color,
    strokeWidth: Float,
    rounded: Boolean = false,
    cornerRadius: Float = 0f,
) /* body omitted */

fun buildRoundedPolylinePath(
    points: List<Pair<Float, Float>>,
    radius: Float
): Path /* body omitted */

fun lerpF(start: Float, stop: Float, fraction: Float): Float /* body omitted */
```

### RoundedSegment (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/draw/RoundedSegment.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays.draw



data class RoundedSegment(
    val points: List<Pair<Float, Float>>,
    val rounded: Boolean,
    val isCorner: Boolean
)
```

### SegmentBuilders (carddetector)

[Source](../../../carddetector/src/main/java/com/apexfission/android/carddetector/ui/overlays/draw/SegmentBuilders.kt)

```kotlin
package com.apexfission.android.carddetector.ui.overlays.draw

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun buildCorner(
    cornerPoint: Pair<Float, Float>,
    cornerLength: Float,
    cornerRadius: Float,
    cornerId: Int
): RoundedSegment /* body omitted */

fun buildCorners(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerLength: Float,
    cornerRadius: Float
): List<RoundedSegment> /* body omitted */

fun buildConnector(
    startCorner: Pair<Float, Float>,
    endCorner: Pair<Float, Float>,
    cornerLength: Float,
    gap: Float,
    sideId: Int
): List<RoundedSegment> /* body omitted */

fun buildConnectors(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerLength: Float,
    gap: Float
): List<RoundedSegment> /* body omitted */
```
