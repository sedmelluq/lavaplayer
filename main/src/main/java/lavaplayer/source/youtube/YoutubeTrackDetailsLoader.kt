package lavaplayer.source.youtube

import lavaplayer.tools.io.HttpInterface

interface YoutubeTrackDetailsLoader {
    fun loadDetails(
        httpInterface: HttpInterface,
        videoId: String,
        requireFormats: Boolean,
        sourceManager: YoutubeItemSourceManager
    ): YoutubeTrackDetails?
}
