package com.sedmelluq.discord.lavaplayer.tools.io

import java.io.IOException
import java.io.InputStream

/**
 * Utility methods for streams.
 */
object StreamTools {
    /**
     * Reads from the stream until either the length number of bytes is read, or the stream ends. Note that neither case
     * throws an exception.
     *
     * @param input  The stream to read from.
     * @param buffer Buffer to write the data that is read from the stream.
     * @param offset Offset in the buffer to start writing from.
     * @param length Maximum number of bytes to read from the stream.
     * @return The number of bytes read from the stream.
     * @throws IOException On read error.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun readUntilEnd(input: InputStream, buffer: ByteArray, offset: Int, length: Int): Int {
        var position = 0
        while (position < length) {
            val count = input.read(buffer, offset + position, length - position)
            if (count < 0) {
                break
            }

            position += count
        }

        return position
    }
}
