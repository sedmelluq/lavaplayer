package com.sedmelluq.discord.lavaplayer.format.transcoder

import java.io.Closeable
import java.nio.ShortBuffer

/**
 * Decodes one chunk of audio into internal PCM format.
 */
interface AudioChunkDecoder : Closeable {
    /**
     * @param encoded Encoded bytes
     * @param output  Output buffer for the PCM data
     */
    fun decode(encoded: ByteArray, output: ShortBuffer)

    /**
     * Frees up all held resources.
     */
    override fun close()
}
