package lavaplayer.format.transcoder

import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * Encodes one chunk of audio from internal PCM format.
 */
interface AudioChunkEncoder {
    /**
     * @param buffer Input buffer containing the PCM samples.
     * @return Encoded bytes
     */
    fun encode(buffer: ShortBuffer): ByteArray

    /**
     * @param input  Input buffer containing the PCM samples.
     * @param output Output buffer to store the encoded bytes in
     */
    fun encode(input: ShortBuffer, output: ByteBuffer)

    /**
     * Frees up all held resources.
     */
    fun close()
}
