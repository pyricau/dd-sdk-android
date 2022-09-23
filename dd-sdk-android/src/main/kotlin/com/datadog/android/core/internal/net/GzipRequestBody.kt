/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.net

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.GzipSink
import okio.Okio

internal class GzipRequestBody(private val body: RequestBody):RequestBody() {

    override fun contentType(): MediaType? {
        return body.contentType()
    }

    override fun contentLength(): Long {
        return -1 // We don't know the compressed length in advance!
    }

    @Suppress("UnsafeThirdPartyFunctionCall") // write to is expected to throw IOExceptions
    override fun writeTo(sink: BufferedSink) {
        val gzipSink: BufferedSink = Okio.buffer(GzipSink(sink))
        body.writeTo(gzipSink)
        gzipSink.close()
    }

}