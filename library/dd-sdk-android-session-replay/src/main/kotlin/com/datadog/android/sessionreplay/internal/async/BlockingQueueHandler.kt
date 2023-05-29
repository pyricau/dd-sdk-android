/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.datadog.android.sessionreplay.internal.processor.RecordedDataProcessor
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue

internal class BlockingQueueHandler(
        private val processor: RecordedDataProcessor,
        private val enricher: BlockingQueueItemEnricher,
        private val executorService: ExecutorService,
) {
    private var workQueue = LinkedBlockingQueue<BlockingQueueItem>()

    @Synchronized
    @MainThread
    internal fun add(systemInformation: SystemInformation): BlockingQueueItem? {
        val blockingQueueItem = enricher.createEnrichedBlockingQueueItem(systemInformation)
                ?: return null

        workQueue.add(blockingQueueItem)
        return blockingQueueItem
    }

    @Synchronized
    @MainThread
    internal fun update() {
        executorService.execute {
            wakeUpProcessor()
        }
    }

    @WorkerThread
    private fun shouldHandleFirstItem(firstItem: BlockingQueueItem): Boolean {
        return firstItem.nodes.isNotEmpty() && firstItem.pendingImages == 0
    }

    @WorkerThread
    private fun wakeUpProcessor() {
        while (workQueue.isNotEmpty()) {
            val firstItem = workQueue.peek() ?: break

            // TODO: clean invalid items

            if (shouldHandleFirstItem(firstItem)) {
                workQueue.remove()
                processor.processScreenSnapshotsAsync(
                        newContext = firstItem.newRumContext,
                        currentContext = firstItem.prevRumContext,
                        timestamp = firstItem.timestamp,
                        nodes = firstItem.nodes,
                        systemInformation = firstItem.systemInformation
                )
                println("yondbg handle first item - queue is now ${workQueue.size}")
            } else {
                println("yondbg dont handle the first item")
                break
            }
        }
    }
}