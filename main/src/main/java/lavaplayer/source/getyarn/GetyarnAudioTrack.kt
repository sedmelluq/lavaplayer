package lavaplayer.source.getyarn

import lavaplayer.container.mpeg.MpegAudioTrack
import lavaplayer.tools.Units
import lavaplayer.tools.io.PersistentHttpStream
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging
import java.net.URI

class GetyarnAudioTrack(trackInfo: AudioTrackInfo, override val sourceManager: GetyarnItemSourceManager) :
    DelegatedAudioTrack(trackInfo) {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            log.debug { "Starting getyarn.io track from URL: ${info.identifier}" }
            PersistentHttpStream(httpInterface, URI(info.identifier), Units.CONTENT_LENGTH_UNKNOWN).use { stream ->
                processDelegate(MpegAudioTrack(info, stream), executor)
            }
        }
    }

    override fun makeShallowClone() =
        GetyarnAudioTrack(info, sourceManager)
}
