/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.utils.forge

import com.datadog.android.core.internal.system.AndroidInfoProvider
import com.datadog.android.core.internal.utils.loggableStackTrace
import com.datadog.android.rum.internal.domain.scope.toErrorSchemaType
import com.datadog.android.rum.model.ErrorEvent
import com.datadog.tools.unit.forge.aThrowable
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory
import fr.xgouchet.elmyr.jvm.ext.aTimestamp
import java.net.URL
import java.util.UUID

internal class ErrorEventForgeryFactory : ForgeryFactory<ErrorEvent> {

    override fun getForgery(forge: Forge): ErrorEvent {
        return ErrorEvent(
            date = forge.aTimestamp(),
            error = ErrorEvent.Error(
                id = forge.aNullable { getForgery<UUID>().toString() },
                message = forge.anAlphabeticalString(),
                source = forge.getForgery(),
                stack = forge.aNullable { aThrowable().loggableStackTrace() },
                resource = forge.aNullable {
                    ErrorEvent.Resource(
                        url = aStringMatching("https://[a-z]+.[a-z]{3}/[a-z0-9_/]+"),
                        method = getForgery(),
                        statusCode = aLong(200, 600),
                        provider = aNullable {
                            ErrorEvent.Provider(
                                domain = aNullable { aStringMatching("[a-z]+\\.[a-z]{3}") },
                                name = aNullable { anAlphabeticalString() },
                                type = aNullable()
                            )
                        }
                    )
                },
                sourceType = forge.aNullable { forge.getForgery() },
                isCrash = forge.aNullable { aBool() },
                type = forge.aNullable { anAlphabeticalString() },
                handling = forge.aNullable { getForgery() },
                handlingStack = forge.aNullable { aThrowable().loggableStackTrace() }
            ),
            view = ErrorEvent.View(
                id = forge.getForgery<UUID>().toString(),
                url = forge.aStringMatching("https://[a-z]+.[a-z]{3}/[a-z0-9_/]+"),
                referrer = forge.aNullable { getForgery<URL>().toString() },
                name = forge.aNullable { anAlphabeticalString() },
                inForeground = forge.aNullable { aBool() }
            ),
            connectivity = forge.aNullable {
                ErrorEvent.Connectivity(
                    status = getForgery(),
                    interfaces = aList { getForgery() },
                    cellular = aNullable {
                        ErrorEvent.Cellular(
                            technology = aNullable { anAlphabeticalString() },
                            carrierName = aNullable { anAlphabeticalString() }
                        )
                    }
                )
            },
            synthetics = forge.aNullable {
                ErrorEvent.Synthetics(
                    testId = forge.anHexadecimalString(),
                    resultId = forge.anHexadecimalString()
                )
            },
            usr = forge.aNullable {
                ErrorEvent.Usr(
                    id = aNullable { anHexadecimalString() },
                    name = aNullable { aStringMatching("[A-Z][a-z]+ [A-Z]\\. [A-Z][a-z]+") },
                    email = aNullable { aStringMatching("[a-z]+\\.[a-z]+@[a-z]+\\.[a-z]{3}") },
                    additionalProperties = exhaustiveAttributes()
                )
            },
            action = forge.aNullable { ErrorEvent.Action(getForgery<UUID>().toString()) },
            application = ErrorEvent.Application(forge.getForgery<UUID>().toString()),
            service = forge.aNullable { anAlphabeticalString() },
            session = ErrorEvent.ErrorEventSession(
                id = forge.getForgery<UUID>().toString(),
                type = ErrorEvent.ErrorEventSessionType.USER,
                hasReplay = forge.aNullable { aBool() }
            ),
            source = forge.aNullable { aValueFrom(ErrorEvent.ErrorEventSource::class.java) },
            ciTest = forge.aNullable {
                ErrorEvent.CiTest(anHexadecimalString())
            },
            os = forge.aNullable {
                val androidInfoProvider = getForgery(AndroidInfoProvider::class.java)
                ErrorEvent.Os(
                    name = androidInfoProvider.osName,
                    version = androidInfoProvider.osVersion,
                    versionMajor = androidInfoProvider.osMajorVersion
                )
            },
            device = forge.aNullable {
                val androidInfoProvider = getForgery(AndroidInfoProvider::class.java)
                ErrorEvent.Device(
                    name = androidInfoProvider.deviceName,
                    model = androidInfoProvider.deviceModel,
                    brand = androidInfoProvider.deviceBrand,
                    type = androidInfoProvider.deviceType.toErrorSchemaType(),
                    architecture = androidInfoProvider.architecture
                )
            },
            context = forge.aNullable {
                ErrorEvent.Context(additionalProperties = forge.exhaustiveAttributes())
            },
            dd = ErrorEvent.Dd(
                session = forge.aNullable { ErrorEvent.DdSession(getForgery()) },
                browserSdkVersion = forge.aNullable { aStringMatching("\\d+\\.\\d+\\.\\d+") }
            )
        )
    }
}
