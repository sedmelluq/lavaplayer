package lavaplayer.container.adts

import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.BaseAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging
import java.io.InputStream

/**
 * Audio track that handles an ADTS packet stream
 *
 * @param trackInfo   Track info
 * @param inputStream Input stream for the ADTS stream
 */
class AdtsAudioTrack(trackInfo: AudioTrackInfo?, private val inputStream: InputStream) : BaseAudioTrack(trackInfo!!) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        val provider = AdtsStreamProvider(inputStream, executor.processingContext)
        try {
            log.debug { "Starting to play ADTS stream $identifier" }
            executor.executeProcessingLoop({ provider.provideFrames() }, null)
        } finally {
            provider.close()
        }
    }
}
