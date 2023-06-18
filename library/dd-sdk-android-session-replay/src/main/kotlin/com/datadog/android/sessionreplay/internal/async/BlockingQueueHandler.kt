/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.datadog.android.sessionreplay.internal.processor.RecordedDataProcessor
import com.datadog.android.sessionreplay.internal.processor.RumContextDataHandler
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import com.datadog.android.sessionreplay.internal.utils.TimeProvider
import java.lang.ClassCastException
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException

internal class BlockingQueueHandler(
    private val processor: RecordedDataProcessor,
    private val rumContextDataHandler: RumContextDataHandler,
    private val executorService: ExecutorService,
    private val timeProvider: TimeProvider
) {
    // region internal

    internal var workQueue = LinkedBlockingQueue<BlockingQueueItem>()

    @MainThread
    internal fun add(systemInformation: SystemInformation): BlockingQueueItem? {
        val rumContextData = rumContextDataHandler.createRumContextData()
            ?: return null

        val blockingQueueItem = BlockingQueueItem(
            timestamp = rumContextData.timestamp,
            prevRumContext = rumContextData.prevRumContext,
            newRumContext = rumContextData.newRumContext,
            systemInformation = systemInformation
        )

        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        try {
            workQueue.offer(blockingQueueItem)
        } catch (e: IllegalArgumentException) {
            // TODO: REPLAY-1364 Add logs here once the sdkLogger is added
        } catch (e: ClassCastException) {
            // in theory will never happen but we'll log it
            // TODO: REPLAY-1364 Add logs here once the sdkLogger is added
        } catch (e: NullPointerException) {
            // in theory will never happen but we'll log it
            // TODO: REPLAY-1364 Add logs here once the sdkLogger is added
        }

        return blockingQueueItem
    }

    @MainThread
    internal fun update() {
        // currentTime needs to be obtained on the uithread
        val currentTime = timeProvider.getDeviceTimestamp()

        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        try {
            executorService.execute {
                wakeUpProcessor(currentTime)
            }
        } catch (e: RejectedExecutionException) {
            // TODO: REPLAY-1364 Add logs here once the sdkLogger is added
        } catch (e: NullPointerException) {
            // in theory will never happen but we'll log it
            // TODO: REPLAY-1364 Add logs here once the sdkLogger is added
        }
    }

    // end region

    // region private

    @WorkerThread
    private fun wakeUpProcessor(currentTime: Long) {
        while (workQueue.isNotEmpty()) {
            val blockingQueueItem = workQueue.peek()

            if (blockingQueueItem != null &&
                (
                    !shouldRemoveItem(blockingQueueItem, currentTime) &&
                        blockingQueueItem.isReady()
                    )
            ) {
                workQueue.poll()
                processor.processScreenSnapshots(
                    newContext = blockingQueueItem.newRumContext,
                    prevContext = blockingQueueItem.prevRumContext,
                    timestamp = blockingQueueItem.timestamp,
                    nodes = blockingQueueItem.nodes,
                    systemInformation = blockingQueueItem.systemInformation
                )
            } else if (shouldRemoveItem(blockingQueueItem, currentTime)) {
                workQueue.poll()
            } else if (blockingQueueItem != null && !blockingQueueItem.isReady()) {
                break
            }
        }
    }

    private fun shouldRemoveItem(blockingQueueItem: BlockingQueueItem?, currentTime: Long) =
        blockingQueueItem != null &&
            (
                !blockingQueueItem.isValid() ||
                    isItemTooOld(
                        currentTime,
                        blockingQueueItem
                    )
                )

    private fun isItemTooOld(
        currentTime: Long,
        blockingQueueItem: BlockingQueueItem
    ): Boolean = (currentTime - blockingQueueItem.timestamp) > MAX_DELAY_MS

    // end region

    internal companion object {
        internal const val MAX_DELAY_MS = 200L
    }
}
