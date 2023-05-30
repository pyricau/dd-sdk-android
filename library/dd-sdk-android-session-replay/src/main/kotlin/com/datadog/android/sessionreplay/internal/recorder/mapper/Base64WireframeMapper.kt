/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.mapper

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import android.view.View
import android.webkit.MimeTypeMap
import com.datadog.android.sessionreplay.internal.recorder.DelayedCallbackInfo
import com.datadog.android.sessionreplay.internal.recorder.GlobalBounds
import com.datadog.android.sessionreplay.internal.recorder.MappingContext
import com.datadog.android.sessionreplay.internal.recorder.image.PixelCopyCapture
import com.datadog.android.sessionreplay.internal.recorder.image.PixelCopyListener
import com.datadog.android.sessionreplay.model.MobileSegment
import java.io.ByteArrayOutputStream

internal class Base64WireframeMapper :
        BaseWireframeMapper<View, MobileSegment.Wireframe.ImageWireframe>() {
    override fun map(
            view: View,
            mappingContext: MappingContext,
            delayedCallbackInfo: DelayedCallbackInfo?
    ):
            List<MobileSegment.Wireframe.ImageWireframe> {

        if (delayedCallbackInfo == null) return emptyList()

        val viewGlobalBounds = resolveViewGlobalBounds(
                view,
                mappingContext.systemInformation.screenDensity
        )

        if (shouldNotCaptureImage(viewGlobalBounds)) return emptyList()

        val (shapeStyle, border) = view.background?.resolveShapeStyleAndBorder(view.alpha)
                ?: (null to null)

        val base64Wireframe = MobileSegment.Wireframe.ImageWireframe(
                id = resolveViewId(view),
                x = viewGlobalBounds.x,
                y = viewGlobalBounds.y,
                width = viewGlobalBounds.width,
                height = viewGlobalBounds.height,
                shapeStyle = shapeStyle,
                border = border,
                base64 = "",
                mimeType = "",
                isEmpty = true
        )

        delayedCallbackInfo.blockingQueueItem.incrementPendingImages()

        captureBitmap(
                view,
                base64Wireframe,
                viewGlobalBounds,
                delayedCallbackInfo
        )

        return listOf(base64Wireframe)
    }

    // region Internal

    private fun createPixelCopyListener(
            base64Wireframe: MobileSegment.Wireframe.ImageWireframe,
            delayedCallbackInfo: DelayedCallbackInfo
    ): PixelCopyListener {
        return object : PixelCopyListener {
            override fun onCopySuccess(bitmap: Bitmap) {
                val encodedBase64String = convert(bitmap)

                if (encodedBase64String.isEmpty()) {
                    delayedCallbackInfo.blockingQueueItem.decrementPendingImages()
                    return
                }

                base64Wireframe.base64 = encodedBase64String
                base64Wireframe.mimeType = getMimeType()
                base64Wireframe.isEmpty = false

                delayedCallbackInfo.blockingQueueItem.decrementPendingImages()
                delayedCallbackInfo.blockingQueueHandler.update()
            }

            override fun onCopyFailure(errorCode: Int) {
                delayedCallbackInfo.blockingQueueItem.decrementPendingImages()
            }
        }
    }

    private fun convert(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()

        val imageFormat = getImageCompressionFormat()

        bitmap.compress(imageFormat, 75, byteArrayOutputStream)
        val bitmapSize  = bitmap.allocationByteCount
        bitmap.recycle()

        return if (isOverSizeLimit(bitmapSize)) {
            ""
        } else {
            encodeBase64ToString(byteArrayOutputStream)
        }
    }

    private fun encodeBase64ToString(byteArrayOutputStream: ByteArrayOutputStream): String {
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun getImageCompressionFormat(): Bitmap.CompressFormat {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }

    private fun getMimeType(): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension("png")
    }

    private fun isOverSizeLimit(bitmapSize: Int): Boolean {
        val bitmapSizeLimit = 15000
        return bitmapSize < bitmapSizeLimit
    }

    private fun captureBitmap(
            view: View,
            base64Wireframe: MobileSegment.Wireframe.ImageWireframe,
            viewGlobalBounds: GlobalBounds,
            delayedCallbackInfo: DelayedCallbackInfo
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activity = getActivity(view) ?: return
            val window = activity.window

            val listener = createPixelCopyListener(base64Wireframe, delayedCallbackInfo)

            val bitmapCapture = PixelCopyCapture(viewGlobalBounds)

            bitmapCapture.getBitmap(
                    view,
                    window,
                    listener
            )
        }
    }

    private fun getActivity(view: View): Activity? {
        var context = view.context ?: return null

        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }

        return null
    }

    private fun shouldNotCaptureImage(viewGlobalBounds: GlobalBounds): Boolean {
        val sizeLimit = 120
        return viewGlobalBounds.width > sizeLimit || viewGlobalBounds.height > sizeLimit
    }

    // endregion
}