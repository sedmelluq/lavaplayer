package lavaplayer.source.local

import lavaplayer.container.MediaContainerDescriptor
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import lavaplayer.track.InternalAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import java.io.File

/**
 * Audio track that handles processing local files as audio tracks.
 *
 * @param trackInfo             Track info
 * @param containerTrackFactory Probe track factory - contains the probe with its parameters.
 * @param sourceManager         Source manager used to load this track
 */
class LocalAudioTrack(
    trackInfo: AudioTrackInfo,
    /**
     * The media probe which handles creating a container-specific delegated track for this track.
     */
    val containerTrackFactory: MediaContainerDescriptor,
    override val sourceManager: LocalItemSourceManager
) : DelegatedAudioTrack(trackInfo) {
    private val file = File(trackInfo.identifier)

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        LocalSeekableInputStream(file).use { inputStream ->
            processDelegate(
                (containerTrackFactory.createTrack(
                    info,
                    inputStream
                ) as InternalAudioTrack), executor
            )
        }
    }

    override fun makeShallowClone(): AudioTrack {
        return LocalAudioTrack(info, containerTrackFactory, sourceManager)
    }
}
