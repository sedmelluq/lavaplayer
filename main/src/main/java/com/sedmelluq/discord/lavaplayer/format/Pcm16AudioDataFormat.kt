package com.sedmelluq.discord.lavaplayer.format

import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkDecoder
import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkEncoder
import com.sedmelluq.discord.lavaplayer.format.transcoder.PcmChunkDecoder
import com.sedmelluq.discord.lavaplayer.format.transcoder.PcmChunkEncoder
import com.sedmelluq.discord.lavaplayer.manager.AudioConfiguration

/**
 * An [AudioDataFormat] for 16-bit signed PCM.
 *
 * @param channelCount     Number of channels.
 * @param sampleRate       Sample rate (frequency).
 * @param chunkSampleCount Number of samples in one chunk.
 * @param bigEndian        Whether the samples are in big-endian format (as opposed to little-endian).
 */
class Pcm16AudioDataFormat(channelCount: Int, sampleRate: Int, chunkSampleCount: Int, private val bigEndian: Boolean) :
    AudioDataFormat(channelCount, sampleRate, chunkSampleCount) {
    companion object {
        const val CODEC_NAME_BE = "PCM_S16_BE"
        const val CODEC_NAME_LE = "PCM_S16_LE"
    }

    private val silenceBytes: ByteArray = ByteArray(channelCount * chunkSampleCount * 2)

    override val codecName: String
        get() = CODEC_NAME_BE

    override val expectedChunkSize: Int
        get() = silenceBytes.size

    override val maximumChunkSize: Int
        get() = silenceBytes.size

    override fun silenceBytes(): ByteArray {
        return silenceBytes
    }

    override fun createDecoder(): AudioChunkDecoder {
        return PcmChunkDecoder(this, bigEndian)
    }

    override fun createEncoder(configuration: AudioConfiguration): AudioChunkEncoder {
        return PcmChunkEncoder(this, bigEndian)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && javaClass == other.javaClass && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + bigEndian.hashCode()
        return result
    }
}
