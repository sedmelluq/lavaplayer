package lavaplayer.tools.io

import java.io.BufferedInputStream
import java.io.InputStream

/**
 * A buffered input stream that gives with the ability to get the number of bytes left in the buffer and a method for
 * discarding the buffer.
 */
class ExtendedBufferedInputStream : BufferedInputStream {
    /**
     * @param `in` Underlying input stream
     */
    constructor(input: InputStream) : super(input)

    /**
     * @param `in`   Underlying input stream
     * @param size Size of the buffer
     */
    constructor(input: InputStream, size: Int) : super(input, size)

    /**
     * @return The number of bytes left in the buffer. This is useful for calculating the actual position in the buffer
     * if the position in the underlying buffer is known.
     */
    val bufferedByteCount: Int
        get() = count - pos

    /**
     * Discard the remaining buffer. This should be called after seek has been performed on the underlying stream.
     */
    fun discardBuffer() {
        pos = count
    }
}
