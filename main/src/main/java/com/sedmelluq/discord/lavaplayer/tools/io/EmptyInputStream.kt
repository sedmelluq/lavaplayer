package com.sedmelluq.discord.lavaplayer.tools.io

import java.io.InputStream

/**
 * Represents an empty input stream.
 */
class EmptyInputStream : InputStream() {
    companion object {
        @JvmField
        val INSTANCE = EmptyInputStream()
    }

    override fun available(): Int {
        return 0
    }

    override fun read(): Int {
        return -1
    }
}
