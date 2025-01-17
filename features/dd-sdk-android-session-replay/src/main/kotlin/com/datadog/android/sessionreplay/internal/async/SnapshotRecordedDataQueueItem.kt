/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import com.datadog.android.sessionreplay.internal.processor.RecordedQueuedItemContext
import com.datadog.android.sessionreplay.internal.recorder.Node
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import java.util.concurrent.atomic.AtomicInteger

internal class SnapshotRecordedDataQueueItem(
    recordedQueuedItemContext: RecordedQueuedItemContext,
    internal val systemInformation: SystemInformation
) : RecordedDataQueueItem(recordedQueuedItemContext) {
    internal var nodes = emptyList<Node>()
    internal var pendingJobs = AtomicInteger(0)

    override fun isValid(): Boolean {
        return nodes.isNotEmpty()
    }

    override fun isReady(): Boolean {
        return pendingJobs.get() == 0
    }

    internal fun incrementPendingJobs() = pendingJobs.incrementAndGet()
    internal fun decrementPendingJobs() = pendingJobs.decrementAndGet()
}
