package lavaplayer.format.transcoder

import lavaplayer.format.AudioDataFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * Audio chunk encoder for PCM data.
 *
 * @param format    Target audio format.
 * @param bigEndian Whether the samples are in big-endian format (as opposed to little-endian).
 */
class PcmChunkEncoder(format: AudioDataFormat, bigEndian: Boolean) : AudioChunkEncoder {
    private val encoded: ByteBuffer
    private val encodedAsShort: ShortBuffer

    init {
        encoded = ByteBuffer.allocate(format.maximumChunkSize)
        if (!bigEndian) {
            encoded.order(ByteOrder.LITTLE_ENDIAN)
        }

        encodedAsShort = encoded.asShortBuffer()
    }

    override fun encode(buffer: ShortBuffer): ByteArray {
        buffer.mark()
        encodedAsShort.clear()
        encodedAsShort.put(buffer)
        encoded.clear()
        encoded.limit(encodedAsShort.position() * 2)
        val encodedBytes = ByteArray(encoded.remaining())
        encoded[encodedBytes]
        buffer.reset()
        return encodedBytes
    }

    override fun encode(input: ShortBuffer, output: ByteBuffer) {
        input.mark()
        encodedAsShort.clear()
        encodedAsShort.put(input)
        output.put(encoded.array(), 0, encodedAsShort.position() * 2)
        output.flip()
        input.reset()
    }

    override fun close() {
        // Nothing to close here
    }
}
