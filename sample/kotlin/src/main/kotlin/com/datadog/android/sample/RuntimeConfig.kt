/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sample

object RuntimeConfig {

    private const val LOCALHOST = "http://localhost"

    var logsEndpointUrl: String = LOCALHOST
    var tracesEndpointUrl: String = LOCALHOST
    var rumEndpointUrl: String = LOCALHOST
    var sessionReplayEndpointUrl: String = LOCALHOST

}