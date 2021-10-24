package com.sedmelluq.discord.lavaplayer.container.ogg

import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext
import java.io.Closeable
import java.io.IOException

/**
 * A handler for a specific codec for an OGG stream.
 */
interface OggTrackHandler : Closeable {
    /**
     * Initialises the track stream.
     *
     * @param context Configuration and output information for processing
     * @throws IOException On read error.
     */
    @Throws(IOException::class)
    fun initialise(context: AudioProcessingContext, timecode: Long, desiredTimecode: Long)

    /**
     * Decodes audio frames and sends them to frame consumer.
     *
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun provideFrames()

    /**
     * Seeks to the specified timecode.
     *
     * @param timecode The timecode in milliseconds
     */
    fun seekToTimecode(timecode: Long)
}
