/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Base64
import android.util.DisplayMetrics
import android.view.View
import android.webkit.MimeTypeMap
import com.datadog.android.sessionreplay.internal.recorder.GlobalBounds
import com.datadog.android.sessionreplay.model.MobileSegment
import com.datadog.android.sessionreplay.utils.UniqueIdentifierGenerator
import java.io.ByteArrayOutputStream

class BitmapSerializer {
    internal fun createImageWireframe (
            view: View,
            drawable: Drawable,
            drawableXY: Pair<Long, Long>,
            drawableBounds: GlobalBounds,
            shapeStyle: MobileSegment.ShapeStyle?,
            border: MobileSegment.ShapeBorder?,
    ):
            MobileSegment.Wireframe.ImageWireframe? {

        if (shouldNotCaptureImage(drawableBounds.width, drawableBounds.height)) return null

        val mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(IMAGE_FORMAT)

        val uniqueIdentifierGenerator = UniqueIdentifierGenerator
        val drawableId = uniqueIdentifierGenerator.resolveChildUniqueIdentifier(
                view,
                DRAWABLE_KEY_NAME
        ) ?: return null

        val base64Representation = serialize(drawable, view.resources.displayMetrics)

        return MobileSegment.Wireframe.ImageWireframe(
                id = drawableId,
                x = drawableXY.first,
                y = drawableXY.second,
                width = drawableBounds.width,
                height = drawableBounds.height,
                shapeStyle = shapeStyle,
                border = border,
                base64 = base64Representation,
                mimeType = mimetype,
                isEmpty = false
        )
    }

    private fun serialize(drawable: Drawable, displayMetrics: DisplayMetrics): String {
        val bitmap = createBitmapFromDrawable(drawable, displayMetrics) ?: return ""
        return serializeToBase64(bitmap)
    }

    private fun createBitmapFromDrawable(drawable: Drawable, displayMetrics: DisplayMetrics): Bitmap? {
        val bitmapWidth = if (drawable.intrinsicWidth <= 0) 1 else drawable.intrinsicWidth
        val bitmapHeight = if (drawable.intrinsicHeight <= 0) 1 else drawable.intrinsicHeight
        val bitmap = Bitmap.createBitmap(displayMetrics, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                ?: return null

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun serializeToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = compressBitmap(bitmap)
        return if (isOverSizeLimit(byteArrayOutputStream.size())) ""
        else encodeBase64ToString(byteArrayOutputStream)
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArrayOutputStream {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val imageFormat = getImageCompressionFormat()
        bitmap.compress(imageFormat, IMAGE_QUALITY, byteArrayOutputStream)
        return byteArrayOutputStream
    }

    private fun isOverSizeLimit(bitmapSize: Int): Boolean {
        return bitmapSize > BITMAP_SIZE_LIMIT
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

    private fun shouldNotCaptureImage(width: Long, height: Long): Boolean {
        return width < 1 ||
                width > DRAWABLE_SIZE_LIMIT ||
                height < 1 ||
                height > DRAWABLE_SIZE_LIMIT
    }

    companion object {
        private const val DRAWABLE_KEY_NAME = "drawable"
        private const val IMAGE_FORMAT = "webp"
        private const val DRAWABLE_SIZE_LIMIT = 120
        private const val BITMAP_SIZE_LIMIT = 15000
        private const val IMAGE_QUALITY = 75
    }
}