/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.net

import com.datadog.android.sessionreplay.model.MobileSegment
import com.datadog.android.sessionreplay.processor.EnrichedRecord
import com.datadog.android.sessionreplay.utils.SessionReplayRumContext
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal class BatchToSegmentsMapper {

    fun map(batchData:List<ByteArray>):List<Pair<MobileSegment, JsonObject>>{
        return groupBatchDataIntoSegments(batchData)
    }

    // region Internal

    private fun groupBatchDataIntoSegments(batchData: List<ByteArray>):
            List<Pair<MobileSegment, JsonObject>>{
        return groupBatchDataByRumContext(batchData).mapNotNull transformer@{ entry->
            val records = entry.value.map { it.asJsonObject }
            if(records.isEmpty()){
                return@transformer null
            }
            val startTimestamp = records.first().getAsJsonPrimitive(TIMESTAMP_KEY).asLong
            val stopTimestamp = records.last().getAsJsonPrimitive(TIMESTAMP_KEY).asLong
            val hasFullSnapshotRecord = hasFullSnapshotRecord(records)
            val segment = MobileSegment(
                    MobileSegment.Application(entry.key.applicationId),
                    MobileSegment.Session(entry.key.sessionId),
                    MobileSegment.View(entry.key.viewId),
                    startTimestamp,
                    stopTimestamp,
                    records.size.toLong(),
                    // TODO: RUMM-2518 Find a way or alternative to provide a reliable indexInView
                    0,
                    hasFullSnapshotRecord,
                    MobileSegment.Source.ANDROID,
                    emptyList()
            )
            val segmentAsJsonObject = segment.toJson().asJsonObject
            segmentAsJsonObject.add(RECORDS_KEY, entry.value)
            Pair(segment,segmentAsJsonObject)
        }
    }

    private fun hasFullSnapshotRecord(records: List<JsonObject>) =
            records.firstOrNull{
                it.getAsJsonPrimitive(RECORD_TYPE_KEY).asLong == FULL_SNAPSHOT_RECORD_TYPE
            } != null

    private fun groupBatchDataByRumContext(batchData: List<ByteArray>):
            Map<SessionReplayRumContext, JsonArray> {
        return batchData
                .map { JsonParser.parseString(String(it)).asJsonObject }
                .map {
                    val applicationId = it.get(EnrichedRecord.APPLICATION_ID_KEY).asString
                    val sessionId = it.get(EnrichedRecord.SESSION_ID_KEY).asString
                    val viewId = it.get(EnrichedRecord.VIEW_ID_KEY).asString
                    val context = SessionReplayRumContext(applicationId, sessionId, viewId)
                    val records = it.get(EnrichedRecord.RECORDS_KEY).asJsonArray
                    Pair(context, records)
                }
                .groupBy { it.first }
                .mapValues {
                    it.value.fold(JsonArray()){ acc, pair->
                        acc.addAll(pair.second)
                        acc
                    }
                }

    }

    // endregion

    companion object {
        private const val TIMESTAMP_KEY="timestamp"
        private const val RECORD_TYPE_KEY="type"
        private const val RECORDS_KEY="records"
        private const val FULL_SNAPSHOT_RECORD_TYPE=10L
    }

}