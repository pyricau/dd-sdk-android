/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.net

import com.datadog.android.core.internal.utils.sdkLogger
import com.datadog.android.log.internal.utils.warningWithTelemetry
import okhttp3.*
import java.io.IOException
import kotlin.jvm.Throws
import okio.BufferedSink
import okio.GzipSink
import okio.Okio

/**
 * This interceptor compresses the HTTP request body.
 *
 * This class uses the [GzipSink] to compress the body content.
 */
internal class GzipRequestInterceptor : Interceptor {

    // region Interceptor

    /**
     * Observes, modifies, or short-circuits requests going out and the responses coming back in.
     */
    // let the proceed exception be handled by the caller
    @Suppress("UnsafeThirdPartyFunctionCall", "TooGenericExceptionCaught")
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val body = originalRequest.body()

        return if (body == null ||
                originalRequest.header(HEADER_ENCODING) != null ||
                body is GzipRequestBody||
                body is MultipartBody) {
            chain.proceed(originalRequest)
        }
        else {
            val compressedRequest = try {
                originalRequest.newBuilder()
                    .header(HEADER_ENCODING, ENCODING_GZIP)
                    .method(originalRequest.method(), GzipRequestBody(body))
                    .build()
            } catch (e: Exception) {
                sdkLogger.warningWithTelemetry("Unable to gzip request body", e)
                originalRequest
            }
            chain.proceed(compressedRequest)
        }
    }

    // endregion

    companion object {
        private const val HEADER_ENCODING = "Content-Encoding"
        private const val ENCODING_GZIP = "gzip"
    }
}
