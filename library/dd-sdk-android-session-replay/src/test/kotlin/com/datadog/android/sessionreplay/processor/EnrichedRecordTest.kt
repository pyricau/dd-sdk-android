/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.processor

import com.datadog.android.sessionreplay.model.MobileSegment
import com.datadog.android.sessionreplay.utils.ForgeConfigurator
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.assertj.core.api.Assertions.assertThat
import java.util.*

@Extensions(
        ExtendWith(MockitoExtension::class),
        ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(ForgeConfigurator::class)
internal class EnrichedRecordTest {

    @Test
    fun `M serialize a record to a JSON string W toJson()`(
            @Forgery fakeEnrichedRecord: EnrichedRecord
    ) {
        // When
        val serializedObject = fakeEnrichedRecord.toJson()

        // Then
        val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
        assertThat(jsonObject.get(EnrichedRecord.APPLICATION_ID_KEY).asString)
                .isEqualTo(fakeEnrichedRecord.applicationId)
        assertThat(jsonObject.get(EnrichedRecord.SESSION_ID_KEY).asString)
                .isEqualTo(fakeEnrichedRecord.sessionId)
        assertThat(jsonObject.get(EnrichedRecord.VIEW_ID_KEY).asString)
                .isEqualTo(fakeEnrichedRecord.viewId)
        val records = jsonObject.get(EnrichedRecord.RECORDS_KEY)
                .asJsonArray
                .map { MobileSegment.MobileRecord.fromJson(it.toString()) }
        assertThat(records).containsExactlyInAnyOrder(*fakeEnrichedRecord.records.toTypedArray())
    }

    @Test
    fun `M deserialize a record to a JSON string W toJson()`(forge: Forge) {
        // Given
        val fakeApplicationId = forge.getForgery<UUID>().toString()
        val fakeSessionId = forge.getForgery<UUID>().toString()
        val fakeViewId = forge.getForgery<UUID>().toString()
        val fakeRecords = forge.aList(1) { forge.getForgery<MobileSegment.MobileRecord.MetaRecord>() }
        val fakeRecordsAsJsonArray = fakeRecords
                .map { it.toJson() }
                .fold(JsonArray()) { accumulator, element ->
                    accumulator.add(element)
                    accumulator
                }
        val fakeSerializedEnrichedRecord = JsonObject()
        fakeSerializedEnrichedRecord
                .addProperty(EnrichedRecord.APPLICATION_ID_KEY, fakeApplicationId)
        fakeSerializedEnrichedRecord.addProperty(EnrichedRecord.SESSION_ID_KEY, fakeSessionId)
        fakeSerializedEnrichedRecord.addProperty(EnrichedRecord.VIEW_ID_KEY, fakeViewId)
        fakeSerializedEnrichedRecord.add(EnrichedRecord.RECORDS_KEY, fakeRecordsAsJsonArray)
        val expectedEnrichedRecord = EnrichedRecord(
                fakeApplicationId,
                fakeSessionId,
                fakeViewId,
                fakeRecords)

        // When
        val deserializedEnrichedRecord = EnrichedRecord.fromJson(
                fakeSerializedEnrichedRecord.toString())


        // Then
        assertThat(deserializedEnrichedRecord).usingRecursiveComparison().isEqualTo(expectedEnrichedRecord)
    }

//    private fun JsonObject.toRecord(): MobileSegment.MobileRecord {
//        val elementType = get(TYPE_KEY).asLong
//    }
//
//    companion object {
x//    }
}