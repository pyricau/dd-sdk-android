/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.listener

import android.app.Activity
import android.view.View
import android.view.ViewTreeObserver
import com.datadog.android.sessionreplay.internal.async.BlockingQueueAdapter
import com.datadog.android.sessionreplay.internal.recorder.Debouncer
import com.datadog.android.sessionreplay.internal.recorder.DelayedCallbackInfo
import com.datadog.android.sessionreplay.internal.recorder.SnapshotProducer
import com.datadog.android.sessionreplay.internal.utils.MiscUtils
import java.lang.ref.WeakReference

internal class WindowsOnDrawListener(
        ownerActivity: Activity,
        zOrderedDecorViews: List<View>,
        private val blockingQueueAdapter: BlockingQueueAdapter,
        private val snapshotProducer: SnapshotProducer,
        private val debouncer: Debouncer = Debouncer(),
        private val miscUtils: MiscUtils = MiscUtils
) : ViewTreeObserver.OnDrawListener {
    internal val ownerActivityReference: WeakReference<Activity> = WeakReference(ownerActivity)
    internal val weakReferencedDecorViews: List<WeakReference<View>>

    init {
        weakReferencedDecorViews = zOrderedDecorViews.map { WeakReference(it) }
    }

    override fun onDraw() {
        debouncer.debounce(resolveTakeSnapshotRunnable())
    }

    private fun resolveTakeSnapshotRunnable(): Runnable = Runnable {
        if (weakReferencedDecorViews.isEmpty()) {
            return@Runnable
        }
        val ownerActivity = ownerActivityReference.get() ?: return@Runnable

        // it is very important to have the windows sorted by their z-order
        val systemInformation = miscUtils.resolveSystemInformation(ownerActivity)
        val blockingQueueItemAdapter = blockingQueueAdapter.add(systemInformation) ?: return@Runnable
        val delayedCallbackInfo = DelayedCallbackInfo(
                systemInformation = systemInformation,
                blockingQueueItem = blockingQueueItemAdapter,
                blockingQueueHandler = blockingQueueAdapter
        )
        val nodes = weakReferencedDecorViews
            .mapNotNull { it.get() }
            .mapNotNull {
                snapshotProducer.produce(it, systemInformation, delayedCallbackInfo)
            }

        blockingQueueItemAdapter.setNodes(nodes)
        blockingQueueAdapter.update()
    }
}
