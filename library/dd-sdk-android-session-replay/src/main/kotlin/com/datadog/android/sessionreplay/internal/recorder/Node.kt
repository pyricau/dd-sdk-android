/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder

import com.datadog.android.sessionreplay.model.MobileSegment

data class Node(
    var wireframes: List<MobileSegment.Wireframe>,
    var children: List<Node> = emptyList(),
    var parents: List<MobileSegment.Wireframe> = emptyList()
)
