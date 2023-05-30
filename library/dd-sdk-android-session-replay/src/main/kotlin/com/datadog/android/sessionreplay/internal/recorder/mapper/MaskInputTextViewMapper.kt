/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.mapper

import android.widget.TextView
import androidx.annotation.VisibleForTesting
import com.datadog.android.sessionreplay.SessionReplayPrivacy
import com.datadog.android.sessionreplay.internal.recorder.obfuscator.rules.MaskInputObfuscationRule
import com.datadog.android.sessionreplay.internal.recorder.obfuscator.rules.TextValueObfuscationRule

/**
 * A [WireframeMapper] implementation to map a [TextView] component and apply the
 * [SessionReplayPrivacy.MASK_USER_INPUT] masking rule.
 */
class MaskInputTextViewMapper : TextViewMapper {
    constructor() : super(textValueObfuscationRule = MaskInputObfuscationRule())

    @VisibleForTesting
    internal constructor(
        textValueObfuscationRule: TextValueObfuscationRule
    ) : super(textValueObfuscationRule)
}