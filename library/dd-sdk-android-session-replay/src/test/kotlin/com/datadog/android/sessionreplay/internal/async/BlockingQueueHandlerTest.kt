/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import com.datadog.android.sessionreplay.forge.ForgeConfigurator
import com.datadog.android.sessionreplay.internal.async.BlockingQueueHandler.Companion.MAX_DELAY_MS
import com.datadog.android.sessionreplay.internal.processor.RecordedDataProcessor
import com.datadog.android.sessionreplay.internal.processor.RumContextData
import com.datadog.android.sessionreplay.internal.processor.RumContextDataHandler
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import com.datadog.android.sessionreplay.internal.time.SessionReplayTimeProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(ForgeConfigurator::class)
internal class BlockingQueueHandlerTest {
    lateinit var testedHandler: BlockingQueueHandler

    @Mock
    lateinit var mockProcessor: RecordedDataProcessor

    @Mock
    lateinit var mockRumContextDataHandler: RumContextDataHandler

    @Mock
    lateinit var mockExecutorService: ExecutorService

    @Mock
    lateinit var mockSystemInformation: SystemInformation

    @Mock
    lateinit var mockTimeProvider: SessionReplayTimeProvider

    @Forgery
    lateinit var fakeRumContextData: RumContextData

    @BeforeEach
    fun setup() {
        whenever(mockExecutorService.execute(any())).then {
            (it.arguments[0] as Runnable).run()
            mock<Future<Boolean>>()
        }

        testedHandler = BlockingQueueHandler(
            mockProcessor,
            mockRumContextDataHandler,
            mockExecutorService,
            mockTimeProvider
        )
    }

    @Test
    fun `M use executorService W update()`() {
        // When
        testedHandler.update()

        // Then
        verify(mockExecutorService).execute(any())
    }

    @Test
    fun `M do not call processor W update() { empty workQueue }`() {
        // When
        testedHandler.update()

        // Then
        verifyNoMoreInteractions(mockProcessor)
    }

    @Test
    fun `M do nothing W add() { invalid RumContextData }`() {
        // Given
        whenever(mockRumContextDataHandler.createRumContextData())
            .thenReturn(null)

        // When
        val blockingQueueItem = testedHandler.add(mockSystemInformation)

        // Then
        assertThat(blockingQueueItem).isNull()
    }

    @Test
    fun `M blockingQueueItem contains correct fields W add() { valid RumContextData }`() {
        // Given
        whenever(mockRumContextDataHandler.createRumContextData())
            .thenReturn(fakeRumContextData)
        val currentRumContextData = mockRumContextDataHandler.createRumContextData()

        // When
        val blockingQueueItem = testedHandler.add(mockSystemInformation)

        // Then
        assertThat(blockingQueueItem!!.prevRumContext).isEqualTo(currentRumContextData?.prevRumContext)
        assertThat(blockingQueueItem.newRumContext).isEqualTo(currentRumContextData?.newRumContext)
        assertThat(blockingQueueItem.timestamp).isEqualTo(currentRumContextData?.timestamp)
        assertThat(blockingQueueItem.systemInformation).isEqualTo(mockSystemInformation)
        assertThat(testedHandler.workQueue.size).isEqualTo(1)
    }

    @Test
    fun `M remove item from queue W update() { expired item }`() {
        // Given
        val spy = spy(
            BlockingQueueItem(
                fakeRumContextData.timestamp,
                fakeRumContextData.newRumContext,
                fakeRumContextData.prevRumContext,
                mockSystemInformation
            )
        )

        doReturn(true).whenever(spy).isValid()
        doReturn(true).whenever(spy).isReady()

        testedHandler.workQueue.add(spy)

        whenever(mockTimeProvider.getDeviceTimestamp())
            .thenReturn(fakeRumContextData.timestamp + MAX_DELAY_MS + 1)

        // When
        testedHandler.update()

        // Then
        assertThat(testedHandler.workQueue.size).isEqualTo(0)
        verifyNoMoreInteractions(mockProcessor)
    }

    @Test
    fun `M remove item from queue W update() { invalid item }`() {
        // Given
        val spy = spy(
            BlockingQueueItem(
                fakeRumContextData.timestamp,
                fakeRumContextData.newRumContext,
                fakeRumContextData.prevRumContext,
                mockSystemInformation
            )
        )

        doReturn(false).whenever(spy).isValid()

        testedHandler.workQueue.add(spy)

        whenever(mockTimeProvider.getDeviceTimestamp())
            .thenReturn(fakeRumContextData.timestamp)

        // When
        testedHandler.update()

        // Then
        assertThat(testedHandler.workQueue.size).isEqualTo(0)
        verifyNoMoreInteractions(mockProcessor)
    }

    @Test
    fun `M do nothing W update() { item not ready }`() {
        // Given
        val spy = spy(
            BlockingQueueItem(
                fakeRumContextData.timestamp,
                fakeRumContextData.newRumContext,
                fakeRumContextData.prevRumContext,
                mockSystemInformation
            )
        )

        doReturn(true).whenever(spy).isValid()
        doReturn(false).whenever(spy).isReady()

        testedHandler.workQueue.add(spy)

        whenever(mockTimeProvider.getDeviceTimestamp())
            .thenReturn(fakeRumContextData.timestamp)

        // When
        testedHandler.update()

        // Then
        verifyNoMoreInteractions(mockProcessor)
    }

    @Test
    fun `M call processor W update() { valid item }`() {
        // Given
        val spy = spy(
            BlockingQueueItem(
                fakeRumContextData.timestamp,
                fakeRumContextData.newRumContext,
                fakeRumContextData.prevRumContext,
                mockSystemInformation
            )
        )

        doReturn(true).whenever(spy).isValid()
        doReturn(true).whenever(spy).isReady()

        testedHandler.workQueue.add(spy)

        whenever(mockTimeProvider.getDeviceTimestamp())
            .thenReturn(fakeRumContextData.timestamp)

        // When
        testedHandler.update()

        // Then
        verify(mockProcessor).processScreenSnapshots(
            nodes = spy.nodes,
            systemInformation = spy.systemInformation,
            newContext = spy.newRumContext,
            prevContext = spy.prevRumContext,
            timestamp = spy.timestamp
        )
    }
}
