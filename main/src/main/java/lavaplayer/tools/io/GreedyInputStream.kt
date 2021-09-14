package lavaplayer.tools.io

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Input stream wrapper which reads or skips until EOF or requested length.
 *
 * @param input Underlying input stream.
 */
class GreedyInputStream(input: InputStream) : FilterInputStream(input) {
    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        var read = 0
        while (read < length) {
            val chunk = `in`.read(buffer, offset + read, length - read)
            if (chunk == -1) {
                return if (read == 0) -1 else read
            }

            read += chunk
        }

        return read
    }

    @Throws(IOException::class)
    override fun skip(maximum: Long): Long {
        var skipped: Long = 0
        while (skipped < maximum) {
            var chunk = `in`.skip(maximum - skipped)
            if (chunk == 0L) {
                chunk = if (`in`.read() == -1) break else 1
            }

            skipped += chunk
        }
        return skipped
    }
}
