/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.utils.forge

import com.datadog.android.rum.internal.domain.event.RumEvent
import com.datadog.android.rum.internal.domain.event.RumEventData
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory
import fr.xgouchet.elmyr.jvm.ext.aTimestamp

internal class RumEventForgeryFactory : ForgeryFactory<RumEvent> {

    override fun getForgery(forge: Forge): RumEvent {
        val eventData = forge.anElementFrom(
                forge.getForgery<RumEventData.Resource>(),
                forge.getForgery<RumEventData.UserAction>(),
                forge.getForgery<RumEventData.View>(),
                forge.getForgery<RumEventData.Error>()
        )

        return RumEvent(
                context = forge.getForgery(),
                timestamp = forge.aTimestamp(),
                eventData = eventData,
                attributes = forge.exhaustiveAttributes(),
                userInfo = forge.getForgery()
        )
    }
}
