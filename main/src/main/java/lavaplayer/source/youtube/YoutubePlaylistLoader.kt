package lavaplayer.source.youtube

import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.AudioTrackFactory

interface YoutubePlaylistLoader {
    var playlistPageCount: Int

    fun load(
        httpInterface: HttpInterface,
        playlistId: String,
        selectedVideoId: String?,
        trackFactory: AudioTrackFactory
    ): AudioTrackCollection
}
