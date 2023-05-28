/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import com.datadog.android.sessionreplay.internal.recorder.Node
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import com.datadog.android.sessionreplay.internal.utils.SessionReplayRumContext

internal data class BlockingQueueItem(
        val timestamp: Long,
        val prevRumContext: SessionReplayRumContext,
        val newRumContext: SessionReplayRumContext,
        val systemInformation: SystemInformation
        ) {
    var nodes = emptyList<Node>()
    var loadingImages = 0
}