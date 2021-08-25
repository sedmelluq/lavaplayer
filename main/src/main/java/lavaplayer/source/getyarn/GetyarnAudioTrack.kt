package lavaplayer.source.getyarn;

import lavaplayer.container.mpeg.MpegAudioTrack
import lavaplayer.source.ItemSourceManager
import lavaplayer.tools.Units
import lavaplayer.tools.io.PersistentHttpStream
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

class GetyarnAudioTrack(trackInfo: AudioTrackInfo, override val sourceManager: GetyarnItemSourceManager) :
    DelegatedAudioTrack(trackInfo) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DelegatedAudioTrack::class.java);
    }

    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            log.debug("Starting getyarn.io track from URL: ${info.identifier}");
            PersistentHttpStream(httpInterface, URI(info.identifier), Units.CONTENT_LENGTH_UNKNOWN).use { stream ->
                processDelegate(MpegAudioTrack(info, stream), executor);
            }
        }
    }

    override fun makeShallowClone() =
        GetyarnAudioTrack(info, sourceManager);
}
