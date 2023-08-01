/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sample

import android.app.Instrumentation
import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
internal class BasicTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(NavActivity::class.java)

    @Test
    fun verifyAppLaunchesSuccessfully() {
        val arguments = InstrumentationRegistry.getArguments()
        val maxItemSizeRate = arguments.getString("maxItemSizeRate")?.toFloat()
        val maxBatchSizeRate = arguments.getString("maxBatchSizeRate")?.toFloat()
        val recentDelayRate = arguments.getString("recentDelayRate")?.toFloat()
        val uploadFrequencyRate = arguments.getString("uploadFrequencyRate")?.toFloat()
        Log.v("GeneticTest", "maxItemSizeRate: $maxItemSizeRate, maxBatchSizeRate: $maxBatchSizeRate, recentDelayRate: $recentDelayRate, uploadFrequencyRate: $uploadFrequencyRate")
        Thread.sleep(10000)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withId(R.id.title)).check(matches(isDisplayed()))
    }
}
