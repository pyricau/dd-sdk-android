/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android

import android.os.Build
import com.datadog.android.core.internal.event.NoOpEventMapper
import com.datadog.android.event.EventMapper
import com.datadog.android.plugin.DatadogPlugin
import com.datadog.android.plugin.Feature as PluginFeature
import com.datadog.android.rum.internal.domain.event.RumEvent
import com.datadog.android.rum.internal.domain.event.RumEventMapper
import com.datadog.android.rum.internal.instrumentation.GesturesTrackingStrategy
import com.datadog.android.rum.internal.instrumentation.GesturesTrackingStrategyApi29
import com.datadog.android.rum.internal.instrumentation.gestures.DatadogGesturesTracker
import com.datadog.android.rum.internal.instrumentation.gestures.GesturesTracker
import com.datadog.android.rum.internal.tracking.JetpackViewAttributesProvider
import com.datadog.android.rum.internal.tracking.UserActionTrackingStrategy
import com.datadog.android.rum.model.ActionEvent
import com.datadog.android.rum.model.ErrorEvent
import com.datadog.android.rum.model.ResourceEvent
import com.datadog.android.rum.model.ViewEvent
import com.datadog.android.rum.tracking.ViewAttributesProvider
import com.datadog.android.rum.tracking.ViewTrackingStrategy

/**
 * An object describing the configuration of the Datadog SDK.
 *
 * This is necessary to initialize the SDK with the [Datadog.initialize] method.
 */
class Configuration
internal constructor(
    internal var coreConfig: Core,
    internal val logsConfig: Feature.Logs?,
    internal val tracesConfig: Feature.Tracing?,
    internal val crashReportConfig: Feature.CrashReport?,
    internal val rumConfig: Feature.RUM?
) {

    internal data class Core(
        var needsClearTextHttp: Boolean,
        val hosts: List<String>
    )

    internal sealed class Feature {
        abstract val endpointUrl: String
        abstract val plugins: List<DatadogPlugin>

        internal data class Logs(
            override val endpointUrl: String,
            override val plugins: List<DatadogPlugin>
        ) : Feature()

        internal data class CrashReport(
            override val endpointUrl: String,
            override val plugins: List<DatadogPlugin>
        ) : Feature()

        internal data class Tracing(
            override val endpointUrl: String,
            override val plugins: List<DatadogPlugin>
        ) : Feature()

        internal data class RUM(
            override val endpointUrl: String,
            override val plugins: List<DatadogPlugin>,
            val samplingRate: Float,
            val gesturesTracker: GesturesTracker?,
            val userActionTrackingStrategy: UserActionTrackingStrategy?,
            val viewTrackingStrategy: ViewTrackingStrategy?,
            val rumEventMapper: EventMapper<RumEvent>
        ) : Feature()
    }

    // region Builder

    /**
     * A Builder class for a [Configuration].
     * @param logsEnabled whether Logs are tracked and sent to Datadog (default: false)
     * @param tracesEnabled whether Spans are tracked and sent to Datadog (default: false)
     * @param crashReportsEnabled whether crashes are tracked and sent to Datadog (default: false)
     * @param rumEnabled whether RUM events are tracked and sent to Datadog (default: false)
     */
    @Suppress("TooManyFunctions")
    class Builder(
        val logsEnabled: Boolean = false,
        val tracesEnabled: Boolean = false,
        val crashReportsEnabled: Boolean = false,
        val rumEnabled: Boolean = false
    ) {
        private var logsConfig: Feature.Logs = DEFAULT_LOGS_CONFIG
        private var tracesConfig: Feature.Tracing = DEFAULT_TRACING_CONFIG
        private var crashReportConfig: Feature.CrashReport = DEFAULT_CRASH_CONFIG
        private var rumConfig: Feature.RUM = DEFAULT_RUM_CONFIG

        private var coreConfig = DEFAULT_CORE_CONFIG

        /**
         * Builds a [Configuration] based on the current state of this Builder.
         */
        fun build(): Configuration {
            return Configuration(
                coreConfig = coreConfig,
                logsConfig = if (logsEnabled) logsConfig else null,
                tracesConfig = if (tracesEnabled) tracesConfig else null,
                crashReportConfig = if (crashReportsEnabled) crashReportConfig else null,
                rumConfig = if (rumEnabled) rumConfig else null
            )
        }

        /**
         * Sets the list of first party hosts.
         * Requests made to a URL with any one of these hosts (or any subdomain) will:
         * - be considered a first party resource and categorised as such in your RUM dashboard;
         * - be wrapped in a Span and have trace id injected to get a full flame-graph in APM.
         * @param hosts a list of all the hosts that you own.
         * See [DatadogInterceptor]
         */
        fun setFirstPartyHosts(hosts: List<String>): Builder {
            coreConfig = coreConfig.copy(hosts = hosts)
            return this
        }

        /**
         * Let the SDK target Datadog's Europe server.
         *
         * Call this if you log on [app.datadoghq.eu](https://app.datadoghq.eu/).
         */
        fun useEUEndpoints(): Builder {
            logsConfig = logsConfig.copy(endpointUrl = DatadogEndpoint.LOGS_EU)
            tracesConfig = tracesConfig.copy(endpointUrl = DatadogEndpoint.TRACES_EU)
            crashReportConfig = crashReportConfig.copy(endpointUrl = DatadogEndpoint.LOGS_EU)
            rumConfig = rumConfig.copy(endpointUrl = DatadogEndpoint.RUM_EU)
            coreConfig = coreConfig.copy(needsClearTextHttp = false)
            return this
        }

        /**
         * Let the SDK target Datadog's US server.
         *
         * Call this if you log on [app.datadoghq.com](https://app.datadoghq.com/).
         */
        fun useUSEndpoints(): Builder {
            logsConfig = logsConfig.copy(endpointUrl = DatadogEndpoint.LOGS_US)
            tracesConfig = tracesConfig.copy(endpointUrl = DatadogEndpoint.TRACES_US)
            crashReportConfig = crashReportConfig.copy(endpointUrl = DatadogEndpoint.LOGS_US)
            rumConfig = rumConfig.copy(endpointUrl = DatadogEndpoint.RUM_US)
            coreConfig = coreConfig.copy(needsClearTextHttp = false)
            return this
        }

        /**
         * Let the SDK target Datadog's Gov server.
         *
         * Call this if you log on [app.ddog-gov.com/](https://app.ddog-gov.com/).
         */
        fun useGovEndpoints(): Builder {
            logsConfig = logsConfig.copy(endpointUrl = DatadogEndpoint.LOGS_GOV)
            tracesConfig = tracesConfig.copy(endpointUrl = DatadogEndpoint.TRACES_GOV)
            crashReportConfig = crashReportConfig.copy(endpointUrl = DatadogEndpoint.LOGS_GOV)
            rumConfig = rumConfig.copy(endpointUrl = DatadogEndpoint.RUM_GOV)
            coreConfig = coreConfig.copy(needsClearTextHttp = false)
            return this
        }

        /**
         * Let the SDK target a custom server for the logs feature.
         */
        fun useCustomLogsEndpoint(endpoint: String): Builder {
            logsConfig = logsConfig.copy(endpointUrl = endpoint)
            checkCustomEndpoint(endpoint)
            return this
        }

        /**
         * Let the SDK target a custom server for the tracing feature.
         */
        fun useCustomTracesEndpoint(endpoint: String): Builder {
            tracesConfig = tracesConfig.copy(endpointUrl = endpoint)
            checkCustomEndpoint(endpoint)
            return this
        }

        /**
         * Let the SDK target a custom server for the crash reports feature.
         */
        fun useCustomCrashReportsEndpoint(endpoint: String): Builder {
            crashReportConfig = crashReportConfig.copy(endpointUrl = endpoint)
            checkCustomEndpoint(endpoint)
            return this
        }

        /**
         * Let the SDK target a custom server for the RUM feature.
         */
        fun useCustomRumEndpoint(endpoint: String): Builder {
            rumConfig = rumConfig.copy(endpointUrl = endpoint)
            checkCustomEndpoint(endpoint)
            return this
        }

        /**
         * Enable the user interaction automatic tracker. By enabling this feature the SDK will intercept
         * UI interaction events (e.g.: taps, scrolls, swipes) and automatically send those as RUM UserActions for you.
         * @param touchTargetExtraAttributesProviders an array with your own implementation of the
         * target attributes provider.
         * @see [ViewAttributesProvider]
         */
        @JvmOverloads
        fun trackInteractions(
            touchTargetExtraAttributesProviders: Array<ViewAttributesProvider> = emptyArray()
        ): Builder {
            val gesturesTracker = gestureTracker(touchTargetExtraAttributesProviders)
            rumConfig = rumConfig.copy(
                gesturesTracker = gesturesTracker,
                userActionTrackingStrategy = provideUserTrackingStrategy(
                    gesturesTracker
                )
            )
            return this
        }

        /**
         * Sets the automatic view tracking strategy used by the SDK.
         * By default no view will be tracked.
         * @param strategy as the [ViewTrackingStrategy]
         * Note: By default, the RUM Monitor will let you handle View events manually.
         * This means that you should call [RumMonitor.startView] and [RumMonitor.stopView]
         * yourself. A view should be started when it becomes visible and interactive
         * (equivalent to `onResume`) and be stopped when it's paused (equivalent to `onPause`).
         * @see [com.datadog.android.rum.tracking.ActivityViewTrackingStrategy]
         * @see [com.datadog.android.rum.tracking.FragmentViewTrackingStrategy]
         * @see [com.datadog.android.rum.tracking.MixedViewTrackingStrategy]
         * @see [com.datadog.android.rum.tracking.NavigationViewTrackingStrategy]

         */
        fun useViewTrackingStrategy(strategy: ViewTrackingStrategy): Builder {
            rumConfig = rumConfig.copy(viewTrackingStrategy = strategy)
            return this
        }

        /**
         * Adds a plugin to a specific feature. This plugin will only be registered if the feature
         * was enabled.
         * @param plugin a [DatadogPlugin]
         * @param feature the feature for which this plugin should be registered
         * @see [Feature.LOG]
         * @see [Feature.CRASH]
         * @see [Feature.TRACE]
         * @see [Feature.RUM]
         */
        fun addPlugin(plugin: DatadogPlugin, feature: PluginFeature): Builder {
            when (feature) {
                PluginFeature.RUM -> rumConfig = rumConfig.copy(
                    plugins = rumConfig.plugins + plugin
                )
                PluginFeature.TRACE -> tracesConfig = tracesConfig.copy(
                    plugins = tracesConfig.plugins + plugin
                )
                PluginFeature.LOG -> logsConfig = logsConfig.copy(
                    plugins = logsConfig.plugins + plugin
                )
                PluginFeature.CRASH -> crashReportConfig = crashReportConfig.copy(
                    plugins = crashReportConfig.plugins + plugin
                )
            }

            return this
        }

        /**
         * Sets the sampling rate for RUM Sessions.
         *
         * @param samplingRate the sampling rate must be a value between 0 and 100. A value of 0
         * means no RUM event will be sent, 100 means all sessions will be kept.
         *
         */
        fun sampleRumSessions(samplingRate: Float): Builder {
            rumConfig = rumConfig.copy(samplingRate = samplingRate)
            return this
        }

        /**
         * Sets the [EventMapper] for the RUM [ViewEvent]. You can use this interface implementation
         * to modify the [ViewEvent] attributes before serialisation.
         *
         * @param eventMapper the [EventMapper] implementation.
         */
        fun setRumViewEventMapper(eventMapper: EventMapper<ViewEvent>): Builder {
            rumConfig = rumConfig.copy(
                rumEventMapper = getRumEventMapper().copy(viewEventMapper = eventMapper)
            )
            return this
        }

        /**
         * Sets the [EventMapper] for the RUM [ResourceEvent]. You can use this interface implementation
         * to modify the [ResourceEvent] attributes before serialisation.
         *
         * @param eventMapper the [EventMapper] implementation.
         */
        fun setRumResourceEventMapper(eventMapper: EventMapper<ResourceEvent>): Builder {
            rumConfig = rumConfig.copy(
                rumEventMapper = getRumEventMapper().copy(resourceEventMapper = eventMapper)
            )
            return this
        }

        /**
         * Sets the [EventMapper] for the RUM [ActionEvent]. You can use this interface implementation
         * to modify the [ActionEvent] attributes before serialisation.
         *
         * @param eventMapper the [EventMapper] implementation.
         */
        fun setRumActionEventMapper(eventMapper: EventMapper<ActionEvent>): Builder {
            rumConfig = rumConfig.copy(
                rumEventMapper = getRumEventMapper().copy(actionEventMapper = eventMapper)
            )
            return this
        }

        /**
         * Sets the [EventMapper] for the RUM [ErrorEvent]. You can use this interface implementation
         * to modify the [ErrorEvent] attributes before serialisation.
         *
         * @param eventMapper the [EventMapper] implementation.
         */
        fun setRumErrorEventMapper(eventMapper: EventMapper<ErrorEvent>): Builder {
            rumConfig = rumConfig.copy(
                rumEventMapper = getRumEventMapper().copy(errorEventMapper = eventMapper)
            )
            return this
        }

        private fun checkCustomEndpoint(endpoint: String) {
            if (endpoint.startsWith("http://")) {
                coreConfig = coreConfig.copy(needsClearTextHttp = true)
            }
        }

        private fun provideUserTrackingStrategy(
            gesturesTracker: GesturesTracker
        ): UserActionTrackingStrategy {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                GesturesTrackingStrategyApi29(gesturesTracker)
            } else {
                GesturesTrackingStrategy(gesturesTracker)
            }
        }

        private fun getRumEventMapper(): RumEventMapper {
            val rumEventMapper = rumConfig.rumEventMapper
            return if (rumEventMapper is RumEventMapper) {
                rumEventMapper
            } else {
                RumEventMapper()
            }
        }

        private fun gestureTracker(
            customProviders: Array<ViewAttributesProvider>
        ): DatadogGesturesTracker {
            val defaultProviders = arrayOf(JetpackViewAttributesProvider())
            val providers = customProviders + defaultProviders
            return DatadogGesturesTracker(providers)
        }
    }

    // endregion

    companion object {
        internal const val DEFAULT_SAMPLING_RATE: Float = 100f

        internal val DEFAULT_CORE_CONFIG = Core(
            needsClearTextHttp = false,
            hosts = emptyList()
        )
        internal val DEFAULT_LOGS_CONFIG = Feature.Logs(
            endpointUrl = DatadogEndpoint.LOGS_US,
            plugins = emptyList()
        )
        internal val DEFAULT_CRASH_CONFIG = Feature.CrashReport(
            endpointUrl = DatadogEndpoint.LOGS_US,
            plugins = emptyList()
        )
        internal val DEFAULT_TRACING_CONFIG = Feature.Tracing(
            endpointUrl = DatadogEndpoint.TRACES_US,
            plugins = emptyList()
        )
        internal val DEFAULT_RUM_CONFIG = Feature.RUM(
            endpointUrl = DatadogEndpoint.RUM_US,
            plugins = emptyList(),
            samplingRate = DEFAULT_SAMPLING_RATE,
            gesturesTracker = null,
            userActionTrackingStrategy = null,
            viewTrackingStrategy = null,
            rumEventMapper = NoOpEventMapper()
        )
    }
}
