/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import com.datadog.android.sessionreplay.internal.recorder.Node
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import com.datadog.android.sessionreplay.internal.utils.SessionReplayRumContext
import java.util.concurrent.atomic.AtomicInteger

internal data class BlockingQueueItem(
    internal val timestamp: Long,
    internal val prevRumContext: SessionReplayRumContext,
    internal val newRumContext: SessionReplayRumContext,
    internal val systemInformation: SystemInformation
) {
    internal var nodes = emptyList<Node>()
    internal var pendingImages = AtomicInteger(0)

    internal fun incrementPendingImages() = pendingImages.incrementAndGet()
    internal fun decrementPendingImages() = pendingImages.decrementAndGet()
    internal fun isValid(): Boolean = nodes.isNotEmpty()
    internal fun isReady(): Boolean = pendingImages.get() == 0
}
