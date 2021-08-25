package lavaplayer.source.youtube

import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.AudioTrackInfo
import java.util.function.Function

interface YoutubePlaylistLoader {
    fun setPlaylistPageCount(playlistPageCount: Int)

    fun load(
        httpInterface: HttpInterface,
        playlistId: String,
        selectedVideoId: String,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): AudioTrackCollection
}
