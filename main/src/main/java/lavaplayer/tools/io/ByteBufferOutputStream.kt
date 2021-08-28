package lavaplayer.tools.io

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * A byte buffer wrapped in an output stream.
 * @param buffer The underlying byte buffer
 */
class ByteBufferOutputStream(private val buffer: ByteBuffer) : OutputStream() {
    @Throws(IOException::class)
    override fun write(b: Int) {
        buffer.put(b.toByte())
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        buffer.put(b, off, len)
    }
}
