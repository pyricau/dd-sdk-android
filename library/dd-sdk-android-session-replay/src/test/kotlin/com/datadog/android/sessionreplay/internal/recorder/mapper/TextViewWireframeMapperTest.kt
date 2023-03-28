/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder.mapper

import android.content.res.ColorStateList
import android.widget.TextView
import com.datadog.android.sessionreplay.forge.ForgeConfigurator
import com.datadog.android.sessionreplay.internal.recorder.aMockTextView
import com.datadog.android.sessionreplay.internal.recorder.densityNormalized
import com.datadog.android.sessionreplay.model.MobileSegment
import com.datadog.android.sessionreplay.utils.StringUtils
import com.datadog.tools.unit.extensions.ApiLevelExtension
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(ApiLevelExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(ForgeConfigurator::class)
internal class TextViewWireframeMapperTest : BaseTextViewWireframeMapperTest() {

    @Test
    fun `M resolve a TextWireframe W map() { TextView with text }`(forge: Forge) {
        // Given
        val fakeText = forge.aString()
        val mockTextView: TextView = forge.aMockTextView().apply {
            whenever(this.text).thenReturn(fakeText)
            whenever(this.typeface).thenReturn(mock())
        }

        // When
        val textWireframes = testedTextWireframeMapper.map(mockTextView, fakeSystemInformation)

        // Then
        val expectedWireframes = mockTextView.toTextWireframes().map { it.copy(text = fakeText) }
        assertThat(textWireframes).isEqualTo(expectedWireframes)
    }

    @Test
    fun `M resolve a TextWireframe W map() { TextView without text, with hint }`(forge: Forge) {
        // Given
        val fakeHintText = forge.aString()
        val fakeHintColor = forge.anInt(min = 0, max = 0xffffff)
        val mockColorStateList: ColorStateList = mock {
            whenever(it.defaultColor).thenReturn(fakeHintColor)
        }
        val mockTextView: TextView = forge.aMockTextView().apply {
            whenever(this.text).thenReturn("")
            whenever(this.hint).thenReturn(fakeHintText)
            whenever(this.hintTextColors).thenReturn(mockColorStateList)
            whenever(this.typeface).thenReturn(mock())
        }

        // When
        val textWireframes = testedTextWireframeMapper.map(mockTextView, fakeSystemInformation)

        // Then
        val expectedWireframes = mockTextView
            .toTextWireframes()
            .map {
                it.copy(
                    text = fakeHintText,
                    textStyle = MobileSegment.TextStyle(
                        "sans-serif",
                        mockTextView.textSize.toLong()
                            .densityNormalized(fakeSystemInformation.screenDensity),
                        StringUtils.formatColorAndAlphaAsHexa(
                            fakeHintColor,
                            OPAQUE_ALPHA_VALUE
                        )
                    )
                )
            }
        assertThat(textWireframes).isEqualTo(expectedWireframes)
    }

    @Test
    fun `M resolve a TextWireframe W map() { TextView without text, with hint, no hint color }`(
        forge: Forge
    ) {
        // Given
        val fakeHintText = forge.aString()
        val fakeTextColor = forge.anInt(min = 0, max = 0xffffff)
        val mockTextView: TextView = forge.aMockTextView().apply {
            whenever(this.text).thenReturn("")
            whenever(this.hint).thenReturn(fakeHintText)
            whenever(this.hintTextColors).thenReturn(null)
            whenever(this.typeface).thenReturn(mock())
            whenever(this.currentTextColor).thenReturn(fakeTextColor)
        }

        // When
        val textWireframes = testedTextWireframeMapper.map(mockTextView, fakeSystemInformation)

        // Then
        val expectedWireframes = mockTextView
            .toTextWireframes()
            .map {
                it.copy(
                    text = fakeHintText,
                    textStyle = MobileSegment.TextStyle(
                        "sans-serif",
                        mockTextView.textSize.toLong()
                            .densityNormalized(fakeSystemInformation.screenDensity),
                        StringUtils.formatColorAndAlphaAsHexa(
                            fakeTextColor,
                            OPAQUE_ALPHA_VALUE
                        )
                    )
                )
            }
        assertThat(textWireframes).isEqualTo(expectedWireframes)
    }
}