/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option

open class RunGeneticTestTask:DefaultTask() {
    @Input
    @Option(option = "uploadFrequencyRate", description = "Upload frequency rate for the test")
    var uploadFrequencyRate:String="0.0"
    @Input
    @Option(option = "maxBatchSizeRate", description = "Max batch size rate for the test")
    var maxtBathSizeRate:String="0.0"
    @Input
    @Option(option = "maxItemSizeRate", description = "Max item size rate for the test")
    var maxItemSizeRate:String="0.0"
    @Input
    @Option(option = "recentDelayRate", description = "Delay rate between batches for the test")
    var recentDelayRate:String="0.0"
}