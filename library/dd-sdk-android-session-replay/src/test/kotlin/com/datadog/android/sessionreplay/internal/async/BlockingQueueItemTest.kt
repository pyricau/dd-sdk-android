/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.async

import com.datadog.android.sessionreplay.forge.ForgeConfigurator
import com.datadog.android.sessionreplay.internal.recorder.SystemInformation
import com.datadog.android.sessionreplay.internal.utils.SessionReplayRumContext
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(ForgeConfigurator::class)
internal class BlockingQueueItemTest {

    @Forgery
    lateinit var fakeContext: SessionReplayRumContext

    @Forgery
    lateinit var fakeSystemInformation: SystemInformation

    lateinit var testedItem: BlockingQueueItem

    @BeforeEach
    fun `set up`(forge: Forge) {
        testedItem = BlockingQueueItem(
            forge.aLong(),
            fakeContext,
            fakeContext,
            fakeSystemInformation
        )
    }

    @Test
    fun `M return false W isValid() { Nodes is empty }`() {
        // Then
        assert(!testedItem.isValid())
    }

    @Test
    fun `M return true W isValid() { Nodes is not empty }`(forge: Forge) {
        // Given
        testedItem.nodes = listOf(forge.getForgery())

        // Then
        assert(testedItem.isValid())
    }

    @Test
    fun `M return false W isReady() { Pending images is greater than 0 }`() {
        // Given
        testedItem.incrementPendingImages()

        // Then
        assert(!testedItem.isReady())
    }

    @Test
    fun `M return true W isReady() { Pending images is 0 }`() {
        // Then
        assert(testedItem.isReady())
    }

    @Test
    fun `M increment pending images W incrementPendingImages()`() {
        // Given
        val initial = testedItem.pendingImages.get()

        // When
        testedItem.incrementPendingImages()

        // Then
        assert(testedItem.pendingImages.get() == initial + 1)
    }

    @Test
    fun `M decrement pending images W decrementPendingImages()`() {
        // Given
        val initial = testedItem.pendingImages.get()

        // When
        testedItem.decrementPendingImages()

        // Then
        assert(testedItem.pendingImages.get() == initial - 1)
    }
}
