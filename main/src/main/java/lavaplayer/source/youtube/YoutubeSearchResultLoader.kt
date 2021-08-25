package lavaplayer.source.youtube

import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import java.util.function.Function

interface YoutubeSearchResultLoader {
    fun loadSearchResult(query: String, trackFactory: Function<AudioTrackInfo, AudioTrack>): AudioItem

    fun getHttpConfiguration(): ExtendedHttpConfigurable
}
