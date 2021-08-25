package lavaplayer.source.http

import lavaplayer.container.MediaContainerDescriptor
import lavaplayer.tools.Units
import lavaplayer.tools.io.PersistentHttpStream
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import lavaplayer.track.InternalAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Audio track that handles processing HTTP addresses as audio tracks.
 *
 * @param trackInfo             Track info
 * @param containerTrackFactory Container track factory - contains the probe with its parameters.
 * @param sourceManager         Source manager used to load this track
 */
class HttpAudioTrack(
    trackInfo: AudioTrackInfo,
    /**
     * The media probe which handles creating a container-specific delegated track for this track.
     */
    val containerTrackFactory: MediaContainerDescriptor,
    override val sourceManager: HttpItemSourceManager
) : DelegatedAudioTrack(trackInfo) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpAudioTrack::class.java)
    }

    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            log.debug("Starting http track from URL: ${info.identifier}")
            PersistentHttpStream(httpInterface, URI(info.identifier), Units.CONTENT_LENGTH_UNKNOWN).use { stream ->
                processDelegate(containerTrackFactory.createTrack(info, stream) as InternalAudioTrack, executor)
            }
        }
    }

    override fun makeShallowClone() =
        HttpAudioTrack(info, containerTrackFactory, sourceManager)
}
