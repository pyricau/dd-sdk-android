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
import com.datadog.android.sessionreplay.model.MobileSegment
import java.lang.ClassCastException
import java.lang.NullPointerException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

/**
 * This class is responsible for storing the [BlockingQueueItem]s in a queue and processing them.
 * These items are added to the queue from the main thread and processed on a background thread
 */
internal class BlockingQueueHandler(
    private val processor: RecordedDataProcessor,
    private val rumContextDataHandler: RumContextDataHandler,
    private val executorService: ExecutorService,
    private val timeProvider: TimeProvider
) {
    // region internal

    internal var workQueue = ConcurrentLinkedQueue<BlockingQueueItem>()

    @MainThread
    internal fun addTouchEventBlockingQueueItem(
        pointerInteractions: List<MobileSegment.MobileRecord>
    ): TouchEventBlockingQueueItem? {

        val rumContextData = rumContextDataHandler.createRumContextData()
            ?: return null

        val blockingQueueItem = TouchEventBlockingQueueItem(
            timestamp = rumContextData.timestamp,
            prevRumContext = rumContextData.prevRumContext,
            newRumContext = rumContextData.newRumContext
        )

        blockingQueueItem.touchData = pointerInteractions

        insertIntoWorkQueue(blockingQueueItem)

        return blockingQueueItem
    }

    @MainThread
    internal fun addSnapshotBlockingQueueItem(
        systemInformation: SystemInformation?
    ): SnapshotBlockingQueueItem? {

        val rumContextData = rumContextDataHandler.createRumContextData()
            ?: return null

        val blockingQueueItem = SnapshotBlockingQueueItem(
            timestamp = rumContextData.timestamp,
            prevRumContext = rumContextData.prevRumContext,
            newRumContext = rumContextData.newRumContext
        )

        blockingQueueItem.systemInformation = systemInformation

        insertIntoWorkQueue(blockingQueueItem)

        return blockingQueueItem
    }

    @MainThread
    internal fun tryToConsumeItems() {
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
    @Synchronized
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

                when (blockingQueueItem) {
                    is SnapshotBlockingQueueItem ->
                        processSnapshotEvent(blockingQueueItem)
                    is TouchEventBlockingQueueItem ->
                        processTouchEvent(blockingQueueItem)
                }
            } else if (shouldRemoveItem(blockingQueueItem, currentTime)) {
                workQueue.poll()
            } else if (blockingQueueItem != null && !blockingQueueItem.isReady()) {
                break
            }
        }
    }

    private fun processSnapshotEvent(blockingQueueItem: SnapshotBlockingQueueItem) {
        processor.processScreenSnapshots(
            newContext = blockingQueueItem.newRumContext,
            prevContext = blockingQueueItem.prevRumContext,
            timestamp = blockingQueueItem.timestamp,
            nodes = blockingQueueItem.nodes,
            systemInformation = blockingQueueItem.systemInformation!!
        )
    }

    private fun processTouchEvent(blockingQueueItem: TouchEventBlockingQueueItem) {
        processor.processTouchEventsRecords(
            newContext = blockingQueueItem.newRumContext,
            touchEventsRecords = blockingQueueItem.touchData
        )
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

    private fun insertIntoWorkQueue(blockingQueueItem: BlockingQueueItem) {
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
    }

    // end region

    internal companion object {
        internal const val MAX_DELAY_MS = 200L
    }
}
