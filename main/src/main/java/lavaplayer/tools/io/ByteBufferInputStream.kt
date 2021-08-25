package lavaplayer.tools.io

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * A byte buffer exposed as an input stream.
 *
 * @param buffer The buffer to read from.
 */
class ByteBufferInputStream(private val buffer: ByteBuffer) : InputStream() {
    @Throws(IOException::class)
    override fun read(): Int {
        return if (buffer.hasRemaining()) {
            (buffer.get() and 0xFF.toByte()).toInt()
        } else {
            -1
        }
    }

    @Throws(IOException::class)
    override fun read(array: ByteArray, offset: Int, length: Int) = if (buffer.hasRemaining()) {
        val chunk = buffer.remaining().coerceAtMost(length)
        buffer[array, offset, length]
        chunk
    } else {
        -1
    }

    @Throws(IOException::class)
    override fun available(): Int =
        buffer.remaining()
}
