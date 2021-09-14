package lavaplayer.source.youtube.music

import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.AudioTrackFactory

interface YoutubeMixLoader {
    fun load(
        httpInterface: HttpInterface,
        mixId: String,
        selectedVideoId: String?,
        trackFactory: AudioTrackFactory
    ): AudioTrackCollection
}
