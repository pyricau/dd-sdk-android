/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import com.datadog.android.sessionreplay.internal.RecordCallback
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import com.datadog.android.sessionreplay.internal.utils.RumContextProvider
import com.datadog.android.sessionreplay.internal.utils.SessionReplayRumContext
import com.datadog.android.sessionreplay.internal.utils.TimeProvider

internal class BlockingQueueItemEnricher(
        private val rumContextProvider: RumContextProvider,
        private val timeProvider: TimeProvider,
        private val recordCallback: RecordCallback,
) {
    private var prevRumContext: SessionReplayRumContext = SessionReplayRumContext()

    internal fun createEnrichedBlockingQueueItem(systemInformation: SystemInformation): BlockingQueueItem? {
        // we will make sure we get the timestamp on the UI thread to avoid time skewing
        val timestamp = timeProvider.getDeviceTimestamp()

        // TODO: RUMM-2426 Fetch the RumContext from the core SDKContext when available
        val newRumContext = rumContextProvider.getRumContext()

        if (newRumContext.isNotValid()) {
            // TODO: RUMM-2397 Add the proper logs here once the sdkLogger will be added
            return null
        }

        // Because the runnable will be executed in another thread it can happen in case there is
        // an exception in the chain that the record cannot be sent. In this case we will have
        // a RUM view with `has_replay:true` but with no actual records. This is a corner case
        // that we discussed with the RUM team and unfortunately and was accepted as there is
        // another safety net logic in the player that handles this situation. Unfortunately this
        // is a constraint that we must accept as this whole `has_replay` logic was thought for
        // the browser SR sdk and not for mobile which handles features inter - communication
        // completely differently. In any case have in mind that after a discussion with the
        // browser team it appears that this situation may arrive also on their end and was
        // accepted.
        recordCallback.onRecordForViewSent(newRumContext.viewId)

        val item = BlockingQueueItem(
                timestamp = timestamp,
                prevRumContext = prevRumContext.copy(),
                newRumContext = newRumContext.copy(),
                systemInformation = systemInformation.copy()
        )

        prevRumContext = newRumContext

        return item
    }
}