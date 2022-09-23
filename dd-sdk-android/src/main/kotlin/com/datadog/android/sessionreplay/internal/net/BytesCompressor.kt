/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.net

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import kotlin.experimental.and

internal class BytesCompressor {

    fun compressBytes(uncompressedData: ByteArray): ByteArray {
        // Create the compressor with highest level of compression
        val compressor = Deflater(6)
        // Give the compressor the data to compress
        compressor.setInput(uncompressedData)
        // Compress the data
        var buf = ByteArray(uncompressedData.size)
        var counter = 1
        var count: Int
        do {
            buf = ByteArray(buf.size * counter)
            count = compressor.deflate(buf, 0, buf.size, Deflater.SYNC_FLUSH)
            counter += 1
        } while (count >= buf.size)
        val bos = ByteArrayOutputStream(count+20)
        bos.write(buf, 0, count)

        compressor.setInput(ByteArray(0))
        compressor.finish()
        buf = ByteArray(12)

        do {
            buf = ByteArray(buf.size * counter)
            count = compressor.deflate(buf, 0, buf.size, Deflater.FULL_FLUSH)
            counter += 1
        } while (count >= buf.size)
        bos.write(buf, 0, count)

        compressor.end()
        println(bytesToHex(bos.toByteArray()))
        // Get the compressed data
        return bos.toByteArray()
    }

    fun compressBytes3(uncompressedData: ByteArray): ByteArray {
        // Create the compressor with highest level of compression
        val compressor = Deflater(6)
        // Give the compressor the data to compress
        compressor.setInput(uncompressedData)
        // Compress the data
        var buf = ByteArray(1024)
        var counter = 1
        var count: Int
        do {
            buf = ByteArray(buf.size * counter)
            count = compressor.deflate(buf, 0, buf.size, Deflater.SYNC_FLUSH)
            counter += 1
        } while (count >= buf.size)

        val bos = ByteArrayOutputStream(count)
        bos.write(buf, 0, count)
        compressor.end()
        bos.write(ByteArray(6))
        println(bytesToHex(bos.toByteArray()))
        // Get the compressed data
        return bos.toByteArray()
    }

    fun compressBytes2(uncompressedData: ByteArray): ByteArray {
        var compressor = Deflater(6)
        // Give the compressor the data to compress
        compressor.setInput(uncompressedData)
        compressor.finish()
        val outputStream = ByteArrayOutputStream(uncompressedData.size)
        var buffer = ByteArray(1024)
        var count:Int
        while (!compressor.finished()) {
            count = compressor.deflate(buffer) // returns the
            // generated
            // code...
            // index
            outputStream.write(buffer, 0, count)
        }
//        compressor = Deflater()
//        compressor.setInput("s".toByteArray())
//        compressor.finish()
//        buffer= ByteArray(14)
//        count = compressor.deflate(buffer, 0, buffer.size, Deflater.SYNC_FLUSH)
//        println(bytesToHex(buffer))
//        outputStream.write(buffer, 0, count)
//        compressor = Deflater()
//        compressor.setInput("s".toByteArray())
//        compressor.finish()
//        buffer= ByteArray(14)
//        count = compressor.deflate(buffer, 0, buffer.size, Deflater.SYNC_FLUSH)
//        println(bytesToHex(buffer))
//        outputStream.write(buffer, 0, count)
//        compressor = Deflater()
//        compressor.setInput("t".toByteArray())
//        compressor.finish()
//        buffer=ByteArray(14)
//        compressor.deflate(buffer, 0, buffer.size, Deflater.SYNC_FLUSH)
//        compressor = Deflater()
//        compressor.setInput("t".toByteArray())
//        compressor.finish()
//        buffer=ByteArray(14)
//        count = compressor.deflate(buffer, 0, buffer.size, Deflater.FULL_FLUSH)
//        println(bytesToHex(buffer))
//        outputStream.write(buffer, 0, count)
//        outputStream.write(ByteArray(6))
        outputStream.close()
//        println(bytesToHex(outputStream.toByteArray()))
        return outputStream.toByteArray()
    }

    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt().and(0xff)
            val index = v.ushr(4)
            hexChars[j * 2] = HEX_ARRAY.get(index)
            val index1 = v.and(0x0F)
            hexChars[j * 2 + 1] = HEX_ARRAY.get(index1)
        }
        return String(hexChars)
    }
}