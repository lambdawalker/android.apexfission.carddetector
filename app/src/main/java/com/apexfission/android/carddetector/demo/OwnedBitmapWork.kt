package com.apexfission.android.carddetector.demo

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Transfers one bitmap into bounded host work; process must neither retain nor recycle it. */
fun CoroutineScope.consumeOwnedBitmap(
    bitmap: Bitmap,
    process: suspend (Bitmap) -> Unit,
): Job {
    val released = AtomicBoolean(false)
    fun release() {
        if (released.compareAndSet(false, true)) bitmap.recycle()
    }
    return try {
        launch(Dispatchers.Default) {
            try { process(bitmap) } finally { release() }
        }.also { job -> job.invokeOnCompletion { release() } }
    } catch (error: Throwable) {
        release()
        throw error
    }
}
