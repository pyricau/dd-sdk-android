/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.net

import com.datadog.android.core.internal.CoreFeature
import com.datadog.android.core.internal.net.DataOkHttpUploaderV2
import com.datadog.android.core.internal.net.UploadStatus
import com.datadog.android.core.internal.system.AndroidInfoProvider
import com.datadog.android.core.internal.utils.devLogger
import com.datadog.android.core.internal.utils.sdkLogger
import com.datadog.android.rum.RumAttributes
import com.datadog.android.sessionreplay.model.MobileSegment
import okhttp3.*
import org.xml.sax.helpers.XMLFilterImpl
import java.io.File
import java.io.FileOutputStream


internal class SessionReplayOkHttpUploader(
        endpoint: String,
        clientToken: String,
        source: String,
        sdkVersion: String,
        callFactory: Call.Factory,
        androidInfoProvider: AndroidInfoProvider,
        private val coreFeature: CoreFeature,
        private val compressor: BytesCompressor = BytesCompressor()
) : DataOkHttpUploaderV2(
        buildUrl(endpoint, TrackType.SESSION_REPLAY),
        clientToken,
        source,
        sdkVersion,
        callFactory,
        CONTENT_TYPE_MUTLIPART_FORM,
        androidInfoProvider,
        sdkLogger) {

    val compressionFile by lazy {
        File(coreFeature.contextRef.get()!!.cacheDir, "compressedData")
    }

    private val tags: String
        get() {
            val elements = mutableListOf(
                    "${RumAttributes.SERVICE_NAME}:${coreFeature.serviceName}",
                    "${RumAttributes.APPLICATION_VERSION}:" +
                            coreFeature.packageVersionProvider.version,
                    "${RumAttributes.SDK_VERSION}:$sdkVersion",
                    "${RumAttributes.ENV}:${coreFeature.envName}"
//                    "datacenter:us1.staging.dog"
            )

            if (coreFeature.variant.isNotEmpty()) {
                elements.add("${RumAttributes.VARIANT}:${coreFeature.variant}")
            }

            return elements.joinToString(",")
        }

    fun upload(mobileSegment: MobileSegment, mobileSegmentAsBinary: ByteArray): UploadStatus {

        val uploadStatus = try {
            executeUploadRequest(mobileSegment, mobileSegmentAsBinary, requestId)
        } catch (e: Throwable) {
            internalLogger.e("Unable to upload batch data.", e)
            UploadStatus.NETWORK_ERROR
        }

        uploadStatus.logStatus(
                uploaderName,
                mobileSegmentAsBinary.size,
                devLogger,
                ignoreInfo = false,
                sendToTelemetry = false,
                requestId = requestId
        )
        uploadStatus.logStatus(
                uploaderName,
                mobileSegmentAsBinary.size,
                internalLogger,
                ignoreInfo = true,
                sendToTelemetry = true,
                requestId = requestId
        )

        return uploadStatus
    }


    @Suppress("UnsafeThirdPartyFunctionCall") // Called within a try/catch block
    private fun executeUploadRequest(
            mobileSegment: MobileSegment,
            mobileSegmentAsBinary: ByteArray,
            requestId: String
    ): UploadStatus {
        if (clientToken.isBlank()) {
            return UploadStatus.INVALID_TOKEN_ERROR
        }
        val request = buildRequest(mobileSegment, mobileSegmentAsBinary, requestId)
        val call = callFactory.newCall(request)
        val response = call.execute()
        response.close()
        val compressBytes = compressor.compressBytes(mobileSegmentAsBinary)
        val binary = if (!compressionFile.exists()) {
            compressionFile.createNewFile()
            compressBytes.slice(0 until compressBytes.size - 6).toByteArray()
        } else {
            compressBytes.slice(2 until compressBytes.size - 6).toByteArray()
        }
        FileOutputStream(compressionFile, true).use {
            it.write(binary)
        }
        return responseCodeToUploadStatus(response.code())
    }

    private fun buildRequest(segment: MobileSegment,
                             segmentAsBinary: ByteArray,
                             requestId: String): Request {
        val builder = Request.Builder()
                .url(buildUrl())
                .post(buildRequestBody(segment, segmentAsBinary))

        buildHeaders(builder, requestId)

        return builder.build()
    }

    private fun buildRequestBody(segment: MobileSegment, segmentAsBinary: ByteArray): RequestBody {
        val compressedData = compressor.compressBytes(segmentAsBinary)
        return MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "segment",
                        segment.session.id,
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                compressedData))
                .addFormDataPart(
                        "application.id",
                        segment.application.id
                )
                .addFormDataPart(
                        "session.id",
                        segment.session.id
                )
                .addFormDataPart(
                        "view.id",
                        segment.view.id
                )
                .addFormDataPart(
                        "has_full_snapshot",
                        segment.hasFullSnapshot.toString()
                )
                .addFormDataPart(
                        "records_count",
                        segment.recordsCount.toString()
                )
                .addFormDataPart(
                        "raw_segment_size",
                        compressedData.size.toString()
                )
                .addFormDataPart(
                        "start",
                        segment.start.toString()
                )
                .addFormDataPart(
                        "end",
                        segment.end.toString()
                )
                .addFormDataPart(
                        "source",
                        segment.source.toJson().asString
                )
                .addFormDataPart(
                        "creation_reason",
                        "view_change"
                )
                .build()
    }


    override fun buildQueryParameters(): Map<String, Any> {
        return mapOf(
                QUERY_PARAM_SOURCE to source,
                QUERY_PARAM_TAGS to tags,
                HEADER_API_KEY.lowercase() to clientToken,
                QUERY_PARAM_EVP_ORIGIN_KEY to source,
                QUERY_PARAM_EVP_ORIGIN_VERSION_KEY to sdkVersion,
                HEADER_REQUEST_ID.lowercase() to requestId,
        )
    }

    companion object {

        private const val QUERY_PARAM_EVP_ORIGIN_VERSION_KEY = "dd-evp-origin-version"
        private const val QUERY_PARAM_EVP_ORIGIN_KEY = "dd-evp-origin"
    }
}