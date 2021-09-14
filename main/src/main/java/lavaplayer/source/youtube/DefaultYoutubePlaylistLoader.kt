package lavaplayer.source.youtube

import lavaplayer.tools.FriendlyException
import lavaplayer.tools.ThumbnailTools
import lavaplayer.tools.Units
import lavaplayer.tools.Units.secondsToMillis
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.json.JsonBrowser
import lavaplayer.track.*
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import java.io.IOException

class DefaultYoutubePlaylistLoader : YoutubePlaylistLoader {
    @Volatile
    override var playlistPageCount = 6

    override fun load(
        httpInterface: HttpInterface,
        playlistId: String,
        selectedVideoId: String?,
        trackFactory: AudioTrackFactory
    ): BasicAudioTrackCollection {
        val post = HttpPost(YoutubeConstants.BROWSE_URL)
        post.entity = StringEntity(YoutubeConstants.BROWSE_PLAYLIST_PAYLOAD.format(playlistId), "UTF-8")

        try {
            httpInterface.execute(post).use { response ->
                HttpClientTools.assertSuccessWithContent(response, "playlist response")
                HttpClientTools.assertJsonContentType(response)
                val json = JsonBrowser.parse(response.entity.content)
                return buildPlaylist(httpInterface, json, selectedVideoId, trackFactory)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    private fun buildPlaylist(
        httpInterface: HttpInterface,
        json: JsonBrowser,
        selectedVideoId: String?,
        trackFactory: AudioTrackFactory
    ): BasicAudioTrackCollection {
        findErrorAlert(json)?.let { message ->
            throw FriendlyException(message, FriendlyException.Severity.COMMON, null)
        }

        val playlistName =
            json["header"]["playlistHeaderRenderer"]["title"]["runs"][0]["text"].text

        val playlistVideoList =
            json["contents"]["singleColumnBrowseResultsRenderer"]["tabs"][0]["tabRenderer"]["content"]["sectionListRenderer"]["contents"][0]["playlistVideoListRenderer"]

        val tracks: MutableList<AudioTrack> = ArrayList()
        var continuationsToken = extractPlaylistTracks(playlistVideoList, tracks, trackFactory)
        var loadCount = 0
        val pageCount = playlistPageCount

        // Also load the next pages, each result gives us a JSON with separate values for list html and next page loader html
        while (continuationsToken != null && ++loadCount < pageCount) {
            val post = HttpPost(YoutubeConstants.BROWSE_URL)
            post.entity = StringEntity(YoutubeConstants.BROWSE_CONTINUATION_PAYLOAD.format(continuationsToken), "UTF-8")

            httpInterface.execute(post).use { response ->
                HttpClientTools.assertSuccessWithContent(response, "playlist response")
                val continuationJson = JsonBrowser.parse(response.entity.content)
                val playlistVideoListPage = continuationJson["continuationContents"]["playlistVideoListContinuation"]
                continuationsToken = extractPlaylistTracks(playlistVideoListPage, tracks, trackFactory)
            }
        }

        return BasicAudioTrackCollection(
            playlistName!!,
            AudioTrackCollectionType.Playlist,
            tracks,
            findSelectedTrack(tracks, selectedVideoId)
        )
    }

    private fun findErrorAlert(jsonResponse: JsonBrowser): String? {
        val alerts = jsonResponse["alerts"]
        if (!alerts.isNull) {
            val textObject = alerts.values()
                .firstOrNull { it["alertRenderer"]["type"].text == "ERROR" }
                ?.let { it["alertRenderer"]["text"] } ?: return null

            return if (!textObject["simpleText"].isNull) {
                textObject["simpleText"].text
            } else {
                textObject["runs"].values()
                    .map { it["text"].text }
                    .joinToString("")
            }
        }

        return null
    }

    private fun findSelectedTrack(tracks: List<AudioTrack>, selectedVideoId: String?): AudioTrack? {
        if (selectedVideoId != null) {
            return tracks.firstOrNull { it.identifier == selectedVideoId }
        }

        return null
    }

    private fun extractPlaylistTracks(
        playlistVideoList: JsonBrowser,
        tracks: MutableList<AudioTrack>,
        trackFactory: AudioTrackFactory
    ): String? {
        val contents = playlistVideoList["contents"].takeUnless { it.isNull }
            ?: return null

        for (track in contents.values()) {
            val item = track["playlistVideoRenderer"]
            val shortBylineText = item["shortBylineText"]

            // If the isPlayable property does not exist, it means the video is removed or private
            // If the shortBylineText property does not exist, it means the Track is Region blocked
            if (!item["isPlayable"].isNull && !shortBylineText.isNull) {
                val videoId = item["videoId"].text

                /* create the audio track. */
                val info = AudioTrackInfo(
                    title = item["title"].let { it["simpleText"].text ?: it["runs"][0]["text"].safeText },
                    author = shortBylineText["runs"][0]["text"].safeText,
                    length = secondsToMillis(item["lengthSeconds"].cast(Units.DURATION_SEC_UNKNOWN)),
                    identifier = videoId!!,
                    uri = "https://www.youtube.com/watch?v=$videoId",
                    artworkUrl = ThumbnailTools.extractYouTube(item, videoId),
                )

                tracks.add(trackFactory.create(info))
            }
        }

        val continuations = playlistVideoList["continuations"][0]["nextContinuationData"]
        if (!continuations.isNull) {
            return continuations["continuation"].text
        }

        return null
    }
}
