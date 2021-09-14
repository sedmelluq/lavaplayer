package lavaplayer.source.youtube

import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioTrackFactory

interface YoutubeSearchResultLoader {
    val httpConfiguration: ExtendedHttpConfigurable

    fun loadSearchResult(query: String, trackFactory: AudioTrackFactory): AudioItem
}
