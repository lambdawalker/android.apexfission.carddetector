package com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.tensorflow.lite.gpu.CompatibilityList
import com.apexfission.android.yolo.engine.buildYoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine.buildCardDetector
import org.junit.Assert
import com.apexfission.android.yolo.tflite.engine.buildInferenceEngine
import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import com.apexfission.android.yolo.tflite.engine.ThreadConfinedInferenceEngine
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TfliteInterpreterOutputOwnershipTest {

    @Test
    fun subsequentInferenceReturnsAnIndependentOutputArray() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val interpreter = buildInferenceEngine(
            context = context,
            modelPath = ModelCatalog.TfLite.modelPath,
            useGpu = false,
        )
        val bitmap = Bitmap.createBitmap(
            interpreter.inputImageWidth,
            interpreter.inputImageWidth,
            Bitmap.Config.ARGB_8888,
        )

        try {
            val firstOutput = interpreter.runInference(bitmap)
            val secondOutput = interpreter.runInference(bitmap)

            assertNotSame(
                "Each inference result must own an independent output array",
                firstOutput,
                secondOutput,
            )
        } finally {
            bitmap.recycle()
            interpreter.close()
        }
    }

    @Test
    fun engineThreadAffinityIsPreservedAcrossLifecycle() = verifyLifecycle(false)

    @Test
    fun gpuRequestedLifecycleWorksOnCompatibleHardware() {
        assumeTrue("Requires GPU-compatible hardware", CompatibilityList().use {
            it.isDelegateSupportedOnThisDevice
        })
        verifyLifecycle(true)
    }

    private fun verifyLifecycle(useGpu: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Consumer tests use public interfaces; private engine/delegate state belongs
        // in the YOLO repository's own tests. GPU selection may fall back to CPU.
        EngineThreadDispatcher().use { worker ->
            val observedThreads = ConcurrentLinkedQueue<Long>()
            val engine = ThreadConfinedInferenceEngine(worker) {
                observedThreads.add(Thread.currentThread().id)
                val delegate = buildInferenceEngine(
                    context, ModelCatalog.TfLite.modelPath, useGpu, sharedDispatcher = worker,
                )
                object : InferenceEngine by delegate {
                    override fun runInference(bitmap: Bitmap): FloatArray {
                        observedThreads.add(Thread.currentThread().id)
                        return delegate.runInference(bitmap)
                    }
                    override fun close() {
                        observedThreads.add(Thread.currentThread().id)
                        delegate.close()
                    }
                }
            }
            val expectedThread = observedThreads.first()
            Assert.assertNotEquals(Thread.currentThread().id, expectedThread)
            val bitmap = Bitmap.createBitmap(engine.inputImageWidth, engine.inputImageWidth, Bitmap.Config.ARGB_8888)
            val callers = Executors.newFixedThreadPool(5)
            try {
                val results = List(5) { callers.submit(Callable {
                    Assert.assertTrue(engine.runInference(bitmap).isNotEmpty())
                }) }
                results.forEach { it.get(30, TimeUnit.SECONDS) }
                engine.close()
                Assert.assertEquals(7, observedThreads.size) // construction, five calls, close
                Assert.assertTrue(observedThreads.all { it == expectedThread })
                Assert.assertTrue(engine.runInference(bitmap).isEmpty())
            } finally {
                callers.shutdownNow()
                engine.close()
                bitmap.recycle()
            }
        }
    }

    @Test(timeout = 60000)
    fun realBuildersComposeOnOneSharedContext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        EngineThreadDispatcher().use { worker ->
            val yolo = buildYoloDetector(
                context, ModelCatalog.TfLite.modelPath, 0.5f, 0.45f, false,
                sharedDispatcher = worker,
            )
            try {
                val cards = buildCardDetector(yolo, cardClasses = setOf(0), sharedDispatcher = worker)
                try {
                    val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
                    try { cards.track(bitmap) } finally { bitmap.recycle() }
                } finally { cards.close() }
            } finally { yolo.close() }
        }
    }
}
