/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.mapper

import android.widget.CheckBox
import com.datadog.android.sessionreplay.internal.recorder.ViewUtils
import com.datadog.android.sessionreplay.internal.utils.StringUtils
import com.datadog.android.sessionreplay.model.MobileSegment

internal class MaskAllCheckBoxMapper(
    textWireframeMapper: TextWireframeMapper,
    stringUtils: StringUtils = StringUtils,
    uniqueIdentifierGenerator: UniqueIdentifierResolver = UniqueIdentifierResolver,
    viewUtils: ViewUtils = ViewUtils()
) : CheckBoxMapper(textWireframeMapper, stringUtils, uniqueIdentifierGenerator, viewUtils) {

    override fun resolveCheckedShapeStyle(view: CheckBox, checkBoxColor: String):
        MobileSegment.ShapeStyle? {
        // in case the MASK_ALL rule is applied we do not want to show the selection in the
        // checkbox related wireframe and in order to achieve that we need to provide
        // a null value for the `ShapeStyle` and only keep the `ShapeBorder`.
        return null
    }
}