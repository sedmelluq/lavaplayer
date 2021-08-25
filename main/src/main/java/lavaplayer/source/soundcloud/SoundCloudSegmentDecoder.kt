package lavaplayer.source.soundcloud

import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.track.playback.AudioProcessingContext
import java.io.IOException
import java.util.function.Supplier

interface SoundCloudSegmentDecoder : AutoCloseable {
    @Throws(IOException::class)
    fun prepareStream(beginning: Boolean)

    @Throws(IOException::class)
    fun resetStream()

    @Throws(InterruptedException::class, IOException::class)
    fun playStream(context: AudioProcessingContext, startPosition: Long, desiredPosition: Long)

    fun interface Factory {
        fun create(nextStreamProvider: Supplier<SeekableInputStream>): SoundCloudSegmentDecoder
    }
}
