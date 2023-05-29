/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.image

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import com.datadog.android.sessionreplay.internal.recorder.MappingContext

internal class PixelCopyCapture: BitmapCapture {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun getBitmap(view: View, mappingContext: MappingContext, window: Window, listener: PixelCopyListener) {
        val locationOnScreen = IntArray(2)
        view.getLocationInWindow(locationOnScreen)

        val x = locationOnScreen[0]
        val y = locationOnScreen[1]

        println("yondbg getBitmap with x: $x and y: $y")

        val scope = Rect(
                x,
                y,
                x + view.width,
                y + view.height
        )

        val dstBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        PixelCopy.request(
                window,
                scope,
                dstBitmap,
                { copyStatus ->
                    if (copyStatus == PixelCopy.SUCCESS) {
                        listener.onCopySuccess(dstBitmap)
                    } else {
                        listener.onCopyFailure(copyStatus)
                    }
                },
                Handler(Looper.getMainLooper())
        )
    }
}
