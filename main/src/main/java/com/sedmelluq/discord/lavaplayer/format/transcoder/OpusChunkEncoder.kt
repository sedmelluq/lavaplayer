package com.sedmelluq.discord.lavaplayer.format.transcoder

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.manager.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.natives.opus.OpusEncoder
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * Audio chunk encoder for Opus codec.
 *
 * @param configuration Audio configuration used for configuring the encoder
 * @param format        Target audio format.
 */
class OpusChunkEncoder(configuration: AudioConfiguration, private val format: AudioDataFormat) : AudioChunkEncoder {
    private val encodedBuffer = ByteBuffer.allocateDirect(format.maximumChunkSize)
    private val encoder = OpusEncoder(format.sampleRate, format.channelCount, configuration.opusEncodingQuality)

    override fun encode(buffer: ShortBuffer): ByteArray {
        encoder.encode(buffer, format.chunkSampleCount, encodedBuffer)
        val bytes = ByteArray(encodedBuffer.remaining())
        encodedBuffer.get(bytes)
        return bytes
    }

    override fun encode(input: ShortBuffer, output: ByteBuffer) {
        if (output.isDirect) {
            encoder.encode(input, format.chunkSampleCount, output)
        } else {
            encoder.encode(input, format.chunkSampleCount, encodedBuffer)
            val length = encodedBuffer.remaining()
            encodedBuffer[output.array(), 0, length]
            output.position(0)
            output.limit(length)
        }
    }

    override fun close() {
        encoder.close()
    }
}
