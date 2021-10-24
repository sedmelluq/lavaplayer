package com.sedmelluq.discord.lavaplayer.source.soundcloud

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3TrackProvider
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext
import java.io.IOException
import java.util.function.Supplier

class SoundCloudMp3SegmentDecoder(
    private val nextStreamProvider: Supplier<SeekableInputStream>
) : SoundCloudSegmentDecoder {
    override fun prepareStream(beginning: Boolean) {
        // Nothing to do.
    }

    override fun resetStream() {
        // Nothing to do.
    }

    @Throws(InterruptedException::class, IOException::class)
    override fun playStream(context: AudioProcessingContext, startPosition: Long, desiredPosition: Long) {
        nextStreamProvider.get().use { stream ->
            val trackProvider = Mp3TrackProvider(context, stream)
            try {
                trackProvider.parseHeaders()
                trackProvider.provideFrames()
            } finally {
                trackProvider.close()
            }
        }
    }

    override fun close() {
        // Nothing to do.
    }
}
