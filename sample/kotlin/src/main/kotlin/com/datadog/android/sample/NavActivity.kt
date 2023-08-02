/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android._InternalProxy
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.event.EventMapper
import com.datadog.android.log.Logs
import com.datadog.android.log.LogsConfiguration
import com.datadog.android.ndk.NdkCrashReports
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.rum.event.ViewEventMapper
import com.datadog.android.rum.model.ActionEvent
import com.datadog.android.rum.model.ErrorEvent
import com.datadog.android.rum.model.LongTaskEvent
import com.datadog.android.rum.model.ResourceEvent
import com.datadog.android.rum.model.ViewEvent
import com.datadog.android.rum.tracking.NavigationViewTrackingStrategy
import com.datadog.android.sample.service.LogsForegroundService
import com.datadog.android.sample.user.UserFragment
import com.datadog.android.sessionreplay.SessionReplay
import com.datadog.android.sessionreplay.SessionReplayConfiguration
import com.datadog.android.sessionreplay.material.MaterialExtensionSupport
import com.datadog.android.trace.AndroidTracer
import com.datadog.android.trace.Trace
import com.datadog.android.trace.TraceConfiguration
import com.google.android.material.snackbar.Snackbar
import io.opentracing.rxjava3.TracingRxJava3Utils
import io.opentracing.util.GlobalTracer
import timber.log.Timber

@Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")
class NavActivity : AppCompatActivity(), TrackingConsentChangeListener {

    lateinit var navController: NavController
    lateinit var rootView: View
    lateinit var appInfoView: TextView
    private val tracedHosts = listOf(
            "datadoghq.com",
            "127.0.0.1"
    )
    // region Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        intent.extras?.let {
            initializeDatadog(it)
        }

        super.onCreate(savedInstanceState)

        Timber.d("onStart")

        setTheme(R.style.Sample_Theme_Custom)

        setContentView(R.layout.activity_nav)
        rootView = findViewById(R.id.frame_container)
        appInfoView = findViewById(R.id.app_info)
    }

    private fun initializeDatadog(bundle: Bundle) {
        val preferences = Preferences.defaultPreferences(this)
        Datadog.setVerbosity(Log.DEBUG)
        Datadog.initialize(
                this,
                createDatadogConfiguration(bundle),
                TrackingConsent.GRANTED
        )

        val rumConfig = createRumConfiguration()
        Rum.enable(rumConfig)

        val sessionReplayConfig = SessionReplayConfiguration.Builder(100f)
                .useCustomEndpoint(RuntimeConfig.sessionReplayEndpointUrl)
                    .addExtensionSupport(MaterialExtensionSupport())
                .build()
        SessionReplay.enable(sessionReplayConfig)

        val logsConfig = LogsConfiguration.Builder().
                useCustomEndpoint(RuntimeConfig.logsEndpointUrl)
                        .build()
        Logs.enable(logsConfig)

        val tracesConfig = TraceConfiguration.Builder()
                .useCustomEndpoint(RuntimeConfig.tracesEndpointUrl)
                .build()
        Trace.enable(tracesConfig)

        NdkCrashReports.enable()

        Datadog.setUserInfo(
                id = preferences.getUserId(),
                name = preferences.getUserName(),
                email = preferences.getUserEmail(),
                extraInfo = mapOf(
                        UserFragment.GENDER_KEY to preferences.getUserGender(),
                        UserFragment.AGE_KEY to preferences.getUserAge()
                )
        )

        GlobalTracer.registerIfAbsent(
                AndroidTracer.Builder()
                        .setService(BuildConfig.APPLICATION_ID)
                        .build()
        )
        GlobalRumMonitor.get().debug = true
        TracingRxJava3Utils.enableTracing(GlobalTracer.get())
    }

    private fun createRumConfiguration(): RumConfiguration {
        return RumConfiguration.Builder(BuildConfig.DD_RUM_APPLICATION_ID)
                .useCustomEndpoint(RuntimeConfig.rumEndpointUrl)
                .useViewTrackingStrategy(
                        NavigationViewTrackingStrategy(
                                R.id.nav_host_fragment,
                                true,
                                SampleNavigationPredicate()
                        )
                )
                .setTelemetrySampleRate(100f)
                .trackUserInteractions()
                .trackLongTasks(250L)
                .setViewEventMapper(object : ViewEventMapper {
                    override fun map(event: ViewEvent): ViewEvent {
                        event.context?.additionalProperties?.put(SampleApplication.ATTR_IS_MAPPED, true)
                        return event
                    }
                })
                .setActionEventMapper(object : EventMapper<ActionEvent> {
                    override fun map(event: ActionEvent): ActionEvent {
                        event.context?.additionalProperties?.put(SampleApplication.ATTR_IS_MAPPED, true)
                        return event
                    }
                })
                .setResourceEventMapper(object : EventMapper<ResourceEvent> {
                    override fun map(event: ResourceEvent): ResourceEvent {
                        event.context?.additionalProperties?.put(SampleApplication.ATTR_IS_MAPPED, true)
                        return event
                    }
                })
                .setErrorEventMapper(object : EventMapper<ErrorEvent> {
                    override fun map(event: ErrorEvent): ErrorEvent {
                        event.context?.additionalProperties?.put(SampleApplication.ATTR_IS_MAPPED, true)
                        return event
                    }
                })
                .setLongTaskEventMapper(object : EventMapper<LongTaskEvent> {
                    override fun map(event: LongTaskEvent): LongTaskEvent {
                        event.context?.additionalProperties?.put(SampleApplication.ATTR_IS_MAPPED, true)
                        return event
                    }
                })
                .build()
    }

    private fun createDatadogConfiguration(bundle: Bundle): Configuration {
        val maxItemSizeRate = bundle.getString("maxItemSizeRate")?.toFloat() ?: 0.0f
        val maxBatchSizeRate = bundle.getString("maxBatchSizeRate")?.toFloat() ?: 0.0f
        val recentDelayRate = bundle.getString("recentDelayRate")?.toFloat() ?: 0.0f
        val uploadFrequencyRate = bundle.getString("uploadFrequencyRate")?.toFloat() ?: 0.0f
        Log.v("Initializing Datadog",
                "maxItemSizeRate: $maxItemSizeRate," +
                        " maxBatchSizeRate: $maxBatchSizeRate, " +
                        "recentDelayRate: $recentDelayRate, " +
                        "uploadFrequencyRate: $uploadFrequencyRate" +
                        "logsEndpointUrl: ${RuntimeConfig.logsEndpointUrl}"+
        "tracesEndpointUrl: ${RuntimeConfig.tracesEndpointUrl}"+
        "rumEndpointUrl: ${RuntimeConfig.rumEndpointUrl}"+
        "sessionReplayEndpointUrl: ${RuntimeConfig.sessionReplayEndpointUrl}")

        val configBuilder = Configuration.Builder(
                clientToken = BuildConfig.DD_CLIENT_TOKEN,
                env = BuildConfig.BUILD_TYPE,
                variant = BuildConfig.FLAVOR
        )
                .setFirstPartyHosts(tracedHosts)
                .setUploadFrequencyRate(uploadFrequencyRate)
                .setMaxBatchSizeRate(maxBatchSizeRate)
                .setMaxItemSizeRate(maxItemSizeRate)
                .setRecentDelayRate(recentDelayRate)
                .apply {
                    _InternalProxy.allowClearTextHttp(this)
                }

//        try {
//            configBuilder.useSite(DatadogSite.valueOf(BuildConfig.DD_SITE_NAME))
//        } catch (e: IllegalArgumentException) {
//            Timber.e("Error setting site to ${BuildConfig.DD_SITE_NAME}")
//        }

        return configBuilder.build()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Timber.d("onRestart")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")

        navController = findNavController(R.id.nav_host_fragment)

        val tracking = Preferences.defaultPreferences(this).getTrackingConsent()
        updateTrackingConsentLabel(tracking)
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.navigation, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var result = true
        when (item.itemId) {
            R.id.set_user_info -> {
                navController.navigate(R.id.fragment_user)
            }

            R.id.show_snack_bar -> {
                Snackbar.make(rootView, LIPSUM, Snackbar.LENGTH_LONG).show()
            }

            R.id.start_foreground_service -> {
                val serviceIntent = Intent(this, LogsForegroundService::class.java)
                startService(serviceIntent)
            }

            R.id.gdpr -> {
                navController.navigate(R.id.fragment_gdpr)
            }

            R.id.clear_all_data -> {
                promptClearAllData()
            }

            else -> result = super.onOptionsItemSelected(item)
        }
        return result
    }

    private fun promptClearAllData() {
        AlertDialog.Builder(this)
                .setMessage(R.string.msg_clear_all_data)
                .setNeutralButton(android.R.string.cancel) { _, _ ->
                    // No Op
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    Datadog.getInstance().clearAllData()
                    Toast.makeText(this, R.string.msg_all_data_cleared, Toast.LENGTH_SHORT).show()
                }
                .create()
                .show()
    }

    override fun onTrackingConsentChanged(trackingConsent: TrackingConsent) =
            updateTrackingConsentLabel(trackingConsent)

    private fun updateTrackingConsentLabel(trackingConsent: TrackingConsent) {
        appInfoView.text = "${BuildConfig.FLAVOR} / Tracking: $trackingConsent"
    }

    // endregion

    companion object {
        internal const val LIPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, …"
    }
}

internal interface TrackingConsentChangeListener {
    fun onTrackingConsentChanged(trackingConsent: TrackingConsent)
}
