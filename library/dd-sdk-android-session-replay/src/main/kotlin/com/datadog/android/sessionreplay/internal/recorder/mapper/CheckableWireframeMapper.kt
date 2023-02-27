/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.mapper

import android.widget.Checkable
import android.widget.TextView
import com.datadog.android.sessionreplay.internal.recorder.GlobalBounds
import com.datadog.android.sessionreplay.internal.recorder.ViewUtils
import com.datadog.android.sessionreplay.model.MobileSegment

internal abstract class CheckableWireframeMapper<T>(
    private val uniqueIdentifierGenerator: UniqueIdentifierResolver,
    viewUtils: ViewUtils
) : BaseWireframeMapper<T, MobileSegment.Wireframe>(viewUtils = viewUtils)
        where T : TextView, T : Checkable {

    internal fun resolveCheckableWireframe(view: T, pixelDensity: Float):
        MobileSegment.Wireframe? {
        val checkboxId = uniqueIdentifierGenerator.resolveChildUniqueIdentifier(
            view,
            CHECKBOX_KEY_NAME
        )
        return if (checkboxId != null) {
            val checkBoxColor = resolveCheckableColor(view)
            val checkBoxBounds = resolveCheckableBounds(view, pixelDensity)
            val shapeStyle = resolveCheckableShapeStyle(view, checkBoxColor)
            val shapeBorder = resolveCheckableShapeBorder(view, checkBoxColor)
            MobileSegment.Wireframe.ShapeWireframe(
                id = checkboxId,
                x = checkBoxBounds.x,
                y = checkBoxBounds.y,
                width = checkBoxBounds.width,
                height = checkBoxBounds.height,
                border = shapeBorder,
                shapeStyle = shapeStyle
            )
        } else {
            null
        }
    }

    internal open fun resolveCheckableShapeStyle(view: T, checkBoxColor: String):
        MobileSegment.ShapeStyle? {
        return if (view.isChecked) {
            MobileSegment.ShapeStyle(
                backgroundColor = checkBoxColor,
                view.alpha
            )
        } else {
            null
        }
    }

    internal open fun resolveCheckableShapeBorder(view: T, checkBoxColor: String):
        MobileSegment.ShapeBorder? {
        return MobileSegment.ShapeBorder(
            color = checkBoxColor,
            width = CHECKBOX_BORDER_WIDTH
        )
    }

    internal abstract fun resolveCheckableColor(view: T): String

    internal abstract fun resolveCheckableBounds(view: T, pixelsDensity: Float): GlobalBounds

    companion object {
        internal const val CHECKBOX_KEY_NAME = "checkbox"
        internal const val CHECKBOX_BORDER_WIDTH = 1L
    }
}