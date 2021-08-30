package lavaplayer.container.wav

import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.BaseAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import org.slf4j.LoggerFactory

/**
 * Audio track that handles a WAV stream
 *
 * @param trackInfo Track info
 * @param stream    Input stream for the WAV file
 */
class WavAudioTrack(trackInfo: AudioTrackInfo, private val stream: SeekableInputStream) : BaseAudioTrack(trackInfo) {
    companion object {
        private val log = LoggerFactory.getLogger(WavAudioTrack::class.java)
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        val trackProvider = WavFileLoader(stream).loadTrack(executor.processingContext)
        try {
            log.debug("Starting to play WAV track {}", identifier)
            executor.executeProcessingLoop({ trackProvider.provideFrames() }) { trackProvider.seekToTimecode(it) }
        } finally {
            trackProvider.close()
        }
    }
}