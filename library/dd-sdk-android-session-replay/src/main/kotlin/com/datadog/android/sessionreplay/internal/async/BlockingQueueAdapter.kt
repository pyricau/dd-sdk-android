/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import com.datadog.android.sessionreplay.internal.recorder.SystemInformation

class BlockingQueueAdapter internal constructor(private val blockingQueueHandler: BlockingQueueHandler) {
    fun update() {
        blockingQueueHandler.update()
    }

    fun add(systemInformation: SystemInformation): BlockingQueueItemAdapter? {
        val item = blockingQueueHandler.add(systemInformation)
        return if (item != null) {
            BlockingQueueItemAdapter(item)
        } else {
            null
        }
    }
}