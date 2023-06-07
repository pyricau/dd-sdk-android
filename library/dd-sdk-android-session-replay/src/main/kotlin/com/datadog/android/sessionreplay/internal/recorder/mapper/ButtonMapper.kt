/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.mapper

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Button
import com.datadog.android.sessionreplay.internal.recorder.DelayedCallbackInfo
import com.datadog.android.sessionreplay.internal.recorder.GlobalBounds
import com.datadog.android.sessionreplay.internal.recorder.MappingContext
import com.datadog.android.sessionreplay.internal.recorder.densityNormalized
import com.datadog.android.sessionreplay.internal.recorder.image.BitmapSerializer
import com.datadog.android.sessionreplay.model.MobileSegment


internal class ButtonMapper(
        private val textWireframeMapper: TextViewMapper = TextViewMapper()
) : BaseWireframeMapper<Button, MobileSegment.Wireframe>() {
    override fun map(
            view: Button,
            mappingContext: MappingContext,
            bitmapSerializer: BitmapSerializer?,
            delayedCallbackInfo: DelayedCallbackInfo?
    ): List<MobileSegment.Wireframe> {

        val result = mutableListOf<MobileSegment.Wireframe>()

        result.addAll(resolveImageWireframes(view, mappingContext, bitmapSerializer))

        val textWireframes = resolveTextWireframes(view, mappingContext, bitmapSerializer, delayedCallbackInfo)
        result.addAll(textWireframes)

        return result.toList()
    }

    // inner region

    private fun resolveTextWireframes(view: Button, mappingContext: MappingContext, bitmapSerializer: BitmapSerializer?, delayedCallbackInfo: DelayedCallbackInfo?):
            List<MobileSegment.Wireframe.TextWireframe> {
        return textWireframeMapper.map(view, mappingContext, bitmapSerializer, delayedCallbackInfo).map {
            if (it.shapeStyle == null && it.border == null) {
                // we were not able to resolve the background for this button so just add a border
                it.copy(border = MobileSegment.ShapeBorder(BLACK_COLOR, 1))
            } else {
                it
            }
        }
    }

    private fun resolveImageWireframes(
            view: Button,
            mappingContext: MappingContext,
            bitmapSerializer: BitmapSerializer?
    ): List<MobileSegment.Wireframe.ImageWireframe> {

        if (bitmapSerializer == null) return emptyList()

        val result = mutableListOf<MobileSegment.Wireframe.ImageWireframe>()

        val (shapeStyle, border) = view.background?.resolveShapeStyleAndBorder(view.alpha)
                ?: (null to null)

        val compoundDrawableLocationMap = HashMap<Int, Pair<Long, Long>>()

        view.compoundDrawables.indices.map{
            val drawable = view.compoundDrawables[it]

            if (drawable != null) {
                val globalBounds = resolveDrawableGlobalBounds(
                        drawable,
                        view,
                        mappingContext.systemInformation.screenDensity
                )
                compoundDrawableLocationMap[it] = when(it) {
                    0 -> getCompoundDrawableStartCoords(view, globalBounds, mappingContext)
                    1 -> getCompoundDrawableTopCoords(view, globalBounds, mappingContext)
                    2 -> getCompoundDrawableEndCoords(view, globalBounds, mappingContext)
                    3 -> getCompoundDrawableBottomCoords(view, globalBounds, mappingContext)
                    else -> throw IllegalStateException("Unexpected drawable index: $it")
                }
            }
        }

        view.compoundDrawables.indices.map {
            val drawable = view.compoundDrawables[it]

            if (drawable != null) {
                val globalBounds = resolveDrawableGlobalBounds(
                        drawable,
                        view,
                        mappingContext.systemInformation.screenDensity
                )

                val wireframe = bitmapSerializer.createImageWireframe(
                        view = view,
                        drawable = drawable,
                        drawableXY = compoundDrawableLocationMap[it]!!,
                        drawableBounds = globalBounds,
                        shapeStyle = shapeStyle,
                        border = border,
                )

                if (wireframe != null) {
                    result.add(wireframe)
                }
            }
        }

        return result.toList()
    }

    private fun getCompoundDrawableStartCoords(view: Button, globalBounds: GlobalBounds, mappingContext: MappingContext): Pair<Long, Long> {
        val startX = globalBounds.x + view.compoundPaddingStart.densityNormalized(mappingContext.systemInformation.screenDensity)
        val startY = globalBounds.y + view.compoundPaddingTop.densityNormalized(mappingContext.systemInformation.screenDensity)
        return Pair(startX, startY)
    }

    private fun getCompoundDrawableTopCoords(view: Button, globalBounds: GlobalBounds, mappingContext: MappingContext): Pair<Long, Long> {
        val topX = globalBounds.x + view.compoundPaddingStart.densityNormalized(mappingContext.systemInformation.screenDensity)
        val topY = globalBounds.y + view.compoundPaddingTop.densityNormalized(mappingContext.systemInformation.screenDensity)
        return Pair(topX, topY)
    }

    private fun getCompoundDrawableEndCoords(view: Button, globalBounds: GlobalBounds, mappingContext: MappingContext): Pair<Long, Long> {
        val endX = globalBounds.x - view.compoundPaddingEnd.densityNormalized(mappingContext.systemInformation.screenDensity)
        val endY = globalBounds.y + view.compoundPaddingTop.densityNormalized(mappingContext.systemInformation.screenDensity)
        return Pair(endX, endY)
    }

    private fun getCompoundDrawableBottomCoords(view: Button, globalBounds: GlobalBounds, mappingContext: MappingContext): Pair<Long, Long> {
        val bottomX = globalBounds.x + view.compoundPaddingStart.densityNormalized(mappingContext.systemInformation.screenDensity)
        val bottomY = globalBounds.y - view.compoundPaddingBottom.densityNormalized(mappingContext.systemInformation.screenDensity)
        return Pair(bottomX, bottomY)
    }

    private fun resolveDrawableGlobalBounds(
            drawable: Drawable,
            view: View,
            pixelsDensity: Float
    ): GlobalBounds {
        val coordinates = IntArray(2)
        // this will always have size >= 2
        @Suppress("UnsafeThirdPartyFunctionCall")
        view.getLocationOnScreen(coordinates)
        val x = coordinates[0].densityNormalized(pixelsDensity).toLong()

        val y = coordinates[1].densityNormalized(pixelsDensity).toLong()

        val height = drawable.intrinsicHeight.densityNormalized(pixelsDensity).toLong()
        val width = drawable.intrinsicWidth.densityNormalized(pixelsDensity).toLong()
        return GlobalBounds(x = x, y = y, height = height, width = width)
    }

    // endregion

    companion object {
        internal const val BLACK_COLOR = "#000000ff"
    }
}
