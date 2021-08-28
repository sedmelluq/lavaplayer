package lavaplayer.format.transcoder

import lavaplayer.format.AudioDataFormat
import lavaplayer.natives.opus.OpusDecoder
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * Audio chunk decoder for Opus codec.
 *
 * @param format Source audio format.
 */
class OpusChunkDecoder(format: AudioDataFormat) : AudioChunkDecoder {
    private val decoder = OpusDecoder(format.sampleRate, format.channelCount)
    private val encodedBuffer: ByteBuffer = ByteBuffer.allocateDirect(4096)

    override fun decode(encoded: ByteArray, output: ShortBuffer) {
        encodedBuffer.clear()
        encodedBuffer.put(encoded)
        encodedBuffer.flip()
        output.clear()
        decoder.decode(encodedBuffer, output)
    }

    override fun close() {
        decoder.close()
    }
}
