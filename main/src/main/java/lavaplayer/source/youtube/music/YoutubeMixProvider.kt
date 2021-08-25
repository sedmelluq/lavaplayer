package lavaplayer.source.youtube.music

import lavaplayer.source.youtube.YoutubeHttpContextFilter.PBJ_PARAMETER
import lavaplayer.tools.DataFormatTools
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.JsonBrowser
import lavaplayer.tools.ThumbnailTools
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.*
import org.apache.http.client.methods.HttpGet
import java.io.IOException
import java.util.function.Function

/**
 * Handles loading of YouTube mixes.
 */
class YoutubeMixProvider : YoutubeMixLoader {
    companion object {
        const val YOUTUBE_MUSIC_MIX = "https://music.youtube.com/playlist?list="
        const val YOUTUBE_MUSIC_VIDEO = "https://music.youtube.com/watch?v="
    }

    /**
     * Loads tracks from mix in parallel into a playlist entry.
     *
     * @param mixId           ID of the mix
     * @param selectedVideoId Selected track, {@link AudioTrackCollection#getSelectedTrack()} will return this.
     * @return Playlist of the tracks in the mix.
     */
    override fun load(
        httpInterface: HttpInterface,
        mixId: String,
        selectedVideoId: String,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): AudioTrackCollection {
        var playlistTitle = "YouTube mix"
        val tracks = mutableListOf<AudioTrack>()
        val mixUrl = "https://www.youtube.com/watch?v=$selectedVideoId&list=$mixId$PBJ_PARAMETER"
        httpInterface.execute(HttpGet(mixUrl)).use { response ->
            try {
                HttpClientTools.assertSuccessWithContent(response, "mix response")

                val body = JsonBrowser.parse(response.entity.content)
                val playlist = body.index(3)["response"]["contents"]["twoColumnWatchNextResults"]["playlist"]["playlist"]
                val title = playlist["title"]
                if (!title.isNull) {
                    playlistTitle = title.safeText
                }

                extractPlaylistTracks(playlist["contents"], tracks, trackFactory)
            } catch (e: IOException) {
                throw FriendlyException("Could not read mix page.", FriendlyException.Severity.SUSPICIOUS, e)
            }
        }

        if (tracks.isEmpty()) {
            throw FriendlyException("Could not find tracks from mix.", FriendlyException.Severity.SUSPICIOUS, null)
        }

        val selectedTrack = findSelectedTrack(tracks, selectedVideoId)
        return BasicAudioTrackCollection(
            playlistTitle,
            AudioTrackCollectionType.Playlist,
            tracks,
            selectedTrack
        )
    }

    private fun extractPlaylistTracks(
        browser: JsonBrowser,
        tracks: MutableList<AudioTrack>,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ) {
        for (renderer in browser.values().map { it["playlistPanelVideoRenderer"] }) {
            val identifier = renderer["videoId"].safeText
            val trackInfo = AudioTrackInfo(
                title = renderer["title"]["simpleText"].safeText,
                author = renderer["longBylineText"]["runs"].index(0)["text"].safeText,
                length = DataFormatTools.parseDuration(renderer["lengthText"]["simpleText"].safeText),
                identifier = identifier,
                isStream = false,
                uri = "https://youtube.com/watch?v=$identifier",
                artworkUrl = ThumbnailTools.extractYouTube(renderer, identifier)
            )

            tracks.add(trackFactory.apply(trackInfo))
        }
    }

    private fun findSelectedTrack(tracks: List<AudioTrack>, selectedVideoId: String?): AudioTrack? {
        if (selectedVideoId != null) {
            return tracks.find { it.identifier.equals(selectedVideoId) }
        }

        return null
    }
}
