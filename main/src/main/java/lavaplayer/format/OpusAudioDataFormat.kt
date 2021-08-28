package lavaplayer.format

import lavaplayer.format.transcoder.AudioChunkDecoder
import lavaplayer.format.transcoder.OpusChunkDecoder
import lavaplayer.manager.AudioConfiguration
import lavaplayer.format.transcoder.AudioChunkEncoder
import lavaplayer.format.transcoder.OpusChunkEncoder

/**
 * An [AudioDataFormat] for OPUS.
 *
 * @param channelCount     Number of channels.
 * @param sampleRate       Sample rate (frequency).
 * @param chunkSampleCount Number of samples in one chunk.
 */
class OpusAudioDataFormat(channelCount: Int, sampleRate: Int, chunkSampleCount: Int) : AudioDataFormat(channelCount, sampleRate, chunkSampleCount) {
    companion object {
        const val CODEC_NAME = "OPUS"
        private val SILENT_OPUS_FRAME = byteArrayOf(0xFC.toByte(), 0xFF.toByte(), 0xFE.toByte())
    }

    override val codecName: String = CODEC_NAME
    override val maximumChunkSize: Int = 32 + 1536 * chunkSampleCount / 960
    override val expectedChunkSize: Int = 32 + 512 * chunkSampleCount / 960

    override fun silenceBytes(): ByteArray {
        return SILENT_OPUS_FRAME
    }

    override fun createDecoder(): AudioChunkDecoder {
        return OpusChunkDecoder(this)
    }

    override fun createEncoder(configuration: AudioConfiguration): AudioChunkEncoder {
        return OpusChunkEncoder(configuration, this)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && javaClass == other.javaClass && super.equals(other)
    }
}
