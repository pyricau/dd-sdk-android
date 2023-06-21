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
 * Responsible for storing the Snapshot and Interaction events in a queue.
 * Allows for asynchronous enrichment, which still preserving the event order.
 * The items are added to the queue from the main thread and processed on a background thread.
 */
internal class RecordedDataQueueHandler(
    private val processor: RecordedDataProcessor,
    private val rumContextDataHandler: RumContextDataHandler,
    private val executorService: ExecutorService,
    private val timeProvider: TimeProvider
) {

    // region internal
    internal val recordedDataQueue = ConcurrentLinkedQueue<RecordedDataQueueItem>()

    @MainThread
    internal fun addTouchEventItem(
        pointerInteractions: List<MobileSegment.MobileRecord>
    ): TouchEventRecordedDataQueueItem? {
        val rumContextData = rumContextDataHandler.createRumContextData()
            ?: return null

        val item = TouchEventRecordedDataQueueItem(
            rumContextData = rumContextData,
            touchData = pointerInteractions
        )

        insertIntoRecordedDataQueue(item)

        return item
    }

    @MainThread
    internal fun addSnapshotItem(
        systemInformation: SystemInformation
    ): SnapshotRecordedDataQueueItem? {
        val rumContextData = rumContextDataHandler.createRumContextData()
            ?: return null

        val item = SnapshotRecordedDataQueueItem(
            rumContextData = rumContextData,
            systemInformation = systemInformation
        )

        insertIntoRecordedDataQueue(item)

        return item
    }

    /**
     * Goes through the queue one item at a time for as long as there are items in the queue.
     * If an item is ready to be consumed, it is processed.
     * If an invalid item is encountered, it is removed (invalid items are possible
     * for example if a snapshot failed to traverse the tree).
     * If neither of the previous conditions occurs, the loop breaks.
     */
    @MainThread
    @Synchronized
    internal fun tryToConsumeItems() {
        // no need to create a thread if the queue is empty
        if (recordedDataQueue.isEmpty()) {
            return
        }

        // currentTime needs to be obtained on the uithread
        val currentTime = timeProvider.getDeviceTimestamp()

        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        try {
            executorService.execute {
                triggerProcessingLoop(currentTime)
            }
        } catch (e: RejectedExecutionException) {
            // TODO: REPLAY-1364 Add logs here once the sdkLogger is added
        } catch (e: NullPointerException) {
            // in theory will never happen but we'll log it
            // TODO: REPLAY-1364 Add logs here once the sdkLogger is added
        }
    }

    @WorkerThread
    private fun triggerProcessingLoop(currentTime: Long) {
        while (recordedDataQueue.isNotEmpty()) {
            val nextItem = recordedDataQueue.peek()

            if (nextItem != null) {
                if (shouldRemoveItem(nextItem, currentTime)) {
                    recordedDataQueue.poll()
                } else if (nextItem.isReady()) {
                    processItem()
                } else {
                    break
                }
            }
        }
    }

    private fun processItem() {
        when (val item = recordedDataQueue.poll()) {
            is SnapshotRecordedDataQueueItem ->
                processSnapshotEvent(item)
            is TouchEventRecordedDataQueueItem ->
                processTouchEvent(item)
        }
    }

    private fun processSnapshotEvent(item: SnapshotRecordedDataQueueItem) {
        processor.processScreenSnapshots(item)
    }

    private fun processTouchEvent(item: TouchEventRecordedDataQueueItem) {
        processor.processTouchEventsRecords(item)
    }

    private fun shouldRemoveItem(recordedDataQueueItem: RecordedDataQueueItem, currentTime: Long) =
        !recordedDataQueueItem.isValid() || isTooOld(currentTime, recordedDataQueueItem)

    private fun isTooOld(currentTime: Long, recordedDataQueueItem: RecordedDataQueueItem): Boolean =
        (currentTime - recordedDataQueueItem.rumContextData.timestamp) > MAX_DELAY_MS

    private fun insertIntoRecordedDataQueue(recordedDataQueueItem: RecordedDataQueueItem) {
        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        try {
            recordedDataQueue.offer(recordedDataQueueItem)
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
