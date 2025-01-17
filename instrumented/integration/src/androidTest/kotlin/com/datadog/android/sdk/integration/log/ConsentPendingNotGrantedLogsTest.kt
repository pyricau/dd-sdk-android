/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sdk.integration.log

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.datadog.android.Datadog
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.sdk.integration.RuntimeConfig
import com.datadog.android.sdk.rules.MockServerActivityTestRule
import com.datadog.tools.unit.ConditionWatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
internal class ConsentPendingNotGrantedLogsTest : LogsTest() {

    @get:Rule
    val mockServerRule = MockServerActivityTestRule(
        ActivityLifecycleLogs::class.java,
        trackingConsent = TrackingConsent.PENDING,
        keepRequests = true
    )

    @Test
    fun verifyAllLogsAreDropped() {
        // update the tracking consent
        Datadog.setTrackingConsent(TrackingConsent.NOT_GRANTED)

        // Wait to make sure all batches are consumed
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        ConditionWatcher {
            val logsPayloads = mockServerRule.getRequests(RuntimeConfig.logsEndpointUrl)
            assertThat(logsPayloads).isEmpty()
            true
        }.doWait(timeoutMs = INITIAL_WAIT_MS)
    }
}
