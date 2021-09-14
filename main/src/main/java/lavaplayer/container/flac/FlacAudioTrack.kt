package lavaplayer.container.flac

import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.BaseAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging

/**
 * Audio track that handles a FLAC stream
 * @param trackInfo Track info
 * @param stream    Input stream for the FLAC file
 */
class FlacAudioTrack(trackInfo: AudioTrackInfo, private val stream: SeekableInputStream) : BaseAudioTrack(trackInfo) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        val file = FlacFileLoader(stream)
        val trackProvider = file.loadTrack(executor.processingContext)
        try {
            log.debug { "Starting to play FLAC track $identifier" }
            executor.executeProcessingLoop({ trackProvider.provideFrames() }) { trackProvider.seekToTimecode(it) }
        } finally {
            trackProvider.close()
        }
    }
}
