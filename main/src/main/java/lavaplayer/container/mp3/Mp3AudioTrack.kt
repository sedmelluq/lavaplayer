package lavaplayer.container.mp3

import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.BaseAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging

/**
 * Audio track that handles an MP3 stream
 *
 * @param trackInfo   Track info
 * @param inputStream Input stream for the MP3 file
 */
class Mp3AudioTrack(trackInfo: AudioTrackInfo?, private val inputStream: SeekableInputStream) : BaseAudioTrack(trackInfo!!) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        val provider = Mp3TrackProvider(executor.processingContext, inputStream)

        try {
            provider.parseHeaders()
            log.debug { "Starting to play MP3 track $identifier" }
            executor.executeProcessingLoop({ provider.provideFrames() }) { timecode: Long ->
                provider.seekToTimecode(timecode)
            }
        } finally {
            provider.close()
        }
    }
}
