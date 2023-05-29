/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.image

import android.view.View
import android.view.Window
import android.widget.ImageView
import com.datadog.android.sessionreplay.internal.recorder.GlobalBounds
import com.datadog.android.sessionreplay.internal.recorder.MappingContext

internal interface BitmapCapture {
    fun getBitmap(view: View, mappingContext: MappingContext, window: Window, listener: PixelCopyListener)
}
