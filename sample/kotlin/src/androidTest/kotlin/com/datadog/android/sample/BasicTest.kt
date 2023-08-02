/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sample

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.datadog.android.Datadog
import com.datadog.android.rum.GlobalRumMonitor
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection

@RunWith(AndroidJUnit4::class)
@LargeTest
internal class BasicTest {

    lateinit var activityScenario: ActivityScenario<NavActivity>
    private val mockWebServer = MockWebServer()
    private var sentDataInKb: Long = 0

    @Before
    fun setUp() {
        InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .cacheDir.deleteRecursively()
        mockWebServer.start()
        mockWebServer.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        sentDataInKb += request.bodySize / 1024
                        Log.v("MockServer", "Received request: ${request.path}")
                        return mockResponse(200)
                    }
                }

        getConnectionUrl().let {
            RuntimeConfig.logsEndpointUrl = "$it/$LOGS_URL_SUFFIX"
            RuntimeConfig.tracesEndpointUrl = "$it/$TRACES_URL_SUFFIX"
            RuntimeConfig.rumEndpointUrl = "$it/$RUM_URL_SUFFIX"
            RuntimeConfig.sessionReplayEndpointUrl = "$it/$SESSION_REPlAY_URL_SUFFIX"
        }


        val arguments = InstrumentationRegistry.getArguments()
        val maxItemSizeRate = arguments.getString("maxItemSizeRate")
        val maxBatchSizeRate = arguments.getString("maxBatchSizeRate")
        val recentDelayRate = arguments.getString("recentDelayRate")
        val uploadFrequencyRate = arguments.getString("uploadFrequencyRate")
        Log.v("GeneticTest", "arguments: $arguments")
        val intent = Intent.makeMainActivity(
                ComponentName(InstrumentationRegistry.getInstrumentation().targetContext, NavActivity::class.java))
                .apply {
                    putExtra("maxItemSizeRate", maxItemSizeRate)
                    putExtra("maxBatchSizeRate", maxBatchSizeRate)
                    putExtra("recentDelayRate", recentDelayRate)
                    putExtra("uploadFrequencyRate", uploadFrequencyRate)
                }
        Log.v("GeneticTest", "maxItemSizeRate: $maxItemSizeRate, maxBatchSizeRate: $maxBatchSizeRate, recentDelayRate: $recentDelayRate, uploadFrequencyRate: $uploadFrequencyRate")
        activityScenario = ActivityScenario.launch(intent)
    }

    @After
    fun tearDown(){
       val cacheDir =  InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir
       val scoreFile = File(cacheDir, "score.txt")
       if(!scoreFile.exists()) {
           scoreFile.createNewFile()
       }
        scoreFile.writeText(sentDataInKb.toString())
    }

    @Test
    fun verifyAppLaunchesSuccessfully() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        repeat(3){
            runAboutUsScenario()
            runSessionReplayInputScenario()
            runImageComponentsScenario()
        }
        Thread.sleep(10000)
    }

    private fun runAboutUsScenario(){
        onView(withId(R.id.navigation_about)).perform(ViewActions.click())
        Thread.sleep(1000)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withId(R.id.about_text)).check(matches(isDisplayed()))
        repeat(4){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(withId(R.id.about_text)).perform(ViewActions.swipeUp())
            onView(withId(R.id.license_text)).perform(ViewActions.swipeDown())
            Thread.sleep(1000)
        }
        repeat(4){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(withId(R.id.about_text)).perform(ViewActions.swipeDown())
            Thread.sleep(1000)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Espresso.pressBack()
    }
    private fun runSessionReplayInputScenario(){
        onView(withId(R.id.navigation_session_replay)).perform(ViewActions.click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1000)
        onView(withId(R.id.navigation_text_view_components)).perform(ViewActions.click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1000)
        Espresso.pressBack()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withId(R.id.navigation_password_edit_text_components)).perform(ViewActions.click())
        onView(withId(R.id.email_view)).perform(ViewActions.typeText("datadog@datadoghq.com"))
        onView(withId(R.id.default_password_view)).perform(ViewActions.typeText("datadog"))
        onView(withId(R.id.web_password_view)).perform(ViewActions.typeText("datadog"))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1000)
        Espresso.pressBack()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Espresso.pressBack()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Espresso.pressBack()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun runImageComponentsScenario(){
        onView(withId(R.id.navigation_session_replay)).perform(ViewActions.click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1000)
        onView(withId(R.id.navigation_image_components)).perform(ViewActions.click())
        Thread.sleep(1000)
        Espresso.pressBack()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Espresso.pressBack()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun mockResponse(code: Int): MockResponse {
        return MockResponse()
                .setResponseCode(code)
                .setBody("{}")
    }

    private fun getConnectionUrl(): String = mockWebServer.url("/").toString().removeSuffix("/")

    companion object {
        const val LOGS_URL_SUFFIX = "logs"
        const val TRACES_URL_SUFFIX = "traces"
        const val RUM_URL_SUFFIX = "rum"
        const val SESSION_REPlAY_URL_SUFFIX = "session-replay"
    }
}
