package lavaplayer.source.soundcloud

import java.util.function.Supplier
import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.source.soundcloud.SoundCloudSegmentDecoder
import lavaplayer.container.ogg.OggPacketInputStream
import lavaplayer.container.ogg.OggTrackBlueprint
import kotlin.Throws
import java.io.IOException
import lavaplayer.container.ogg.OggTrackLoader
import java.lang.InterruptedException
import lavaplayer.track.playback.AudioProcessingContext
import java.lang.Exception

class SoundCloudOpusSegmentDecoder(private val nextStreamProvider: Supplier<SeekableInputStream>) : SoundCloudSegmentDecoder {
    private var lastJoinedStream: OggPacketInputStream? = null
    private var blueprint: OggTrackBlueprint? = null

    @Throws(IOException::class)
    override fun prepareStream(beginning: Boolean) {
        val stream = obtainStream()
        if (beginning) {
            val newBlueprint = OggTrackLoader.loadTrackBlueprint(stream)
            if (blueprint == null) {
                if (newBlueprint == null) {
                    throw IOException("No OGG track detected in the stream.")
                }
                blueprint = newBlueprint
            }
        } else {
            stream.startNewTrack()
        }
    }

    @Throws(IOException::class)
    override fun resetStream() {
        if (lastJoinedStream != null) {
            lastJoinedStream!!.close()
            lastJoinedStream = null
        }
    }

    @Throws(InterruptedException::class, IOException::class)
    override fun playStream(context: AudioProcessingContext, startPosition: Long, desiredPosition: Long) {
        blueprint!!.loadTrackHandler(obtainStream()).use { handler ->
            handler.initialise(
                context,
                startPosition,
                desiredPosition
            )
            handler.provideFrames()
        }
    }

    @Throws(Exception::class)
    override fun close() {
        resetStream()
    }

    private fun obtainStream(): OggPacketInputStream {
        if (lastJoinedStream == null) {
            lastJoinedStream = OggPacketInputStream(nextStreamProvider.get(), true)
        }

        return lastJoinedStream as OggPacketInputStream
    }
}
