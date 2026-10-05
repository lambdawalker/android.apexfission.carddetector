package com.apexfission.android.carddetector.resource

import android.graphics.Bitmap

/** Executes [block] and always recycles this bitmap afterward. */
inline fun <T> Bitmap.use(block: (Bitmap) -> T): T {
    return try {
        block(this)
    } finally {
        if (!isRecycled) recycle()
    }
}
