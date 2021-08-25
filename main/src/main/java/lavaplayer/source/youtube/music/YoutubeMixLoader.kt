package lavaplayer.source.youtube.music

import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.AudioTrackInfo
import java.util.function.Function

interface YoutubeMixLoader {
    fun load(httpInterface: HttpInterface, mixId: String, selectedVideoId: String, trackFactory: Function<AudioTrackInfo, AudioTrack>): AudioTrackCollection
}
