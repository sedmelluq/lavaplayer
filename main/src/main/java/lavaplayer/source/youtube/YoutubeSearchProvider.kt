package lavaplayer.source.youtube

import lavaplayer.tools.DataFormatTools.durationTextToMillis
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.ThumbnailTools
import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpInterfaceManager
import lavaplayer.tools.json.JsonBrowser
import lavaplayer.tools.json.JsonBrowser.Companion.parse
import lavaplayer.track.*
import mu.KotlinLogging
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Handles processing YouTube searches.
 */
class YoutubeSearchProvider : YoutubeSearchResultLoader {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val httpInterfaceManager: HttpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager()

    override val httpConfiguration: ExtendedHttpConfigurable
        get() = httpInterfaceManager

    /**
     * @param query Search query.
     * @return Playlist of the first page of results.
     */
    override fun loadSearchResult(query: String, trackFactory: AudioTrackFactory): AudioItem {
        log.debug { "Performing a search with query $query" }
        try {
            httpInterfaceManager.get().use { httpInterface ->
                val post = HttpPost(YoutubeConstants.SEARCH_URL)
                post.entity = StringEntity(YoutubeConstants.SEARCH_PAYLOAD.format(query.replace("\"", "\\\"")), "UTF-8")

                httpInterface.execute(post).use { response ->
                    HttpClientTools.assertSuccessWithContent(response, "search response")
                    val responseText = EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
                    val jsonBrowser = parse(responseText)
                    return extractSearchResults(jsonBrowser, query, trackFactory)
                }
            }
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyException(e)
        }
    }

    private fun extractSearchResults(
        jsonBrowser: JsonBrowser,
        query: String,
        trackFactory: AudioTrackFactory
    ): AudioItem {
        log.debug { "Attempting to parse results from search page" }

        val tracks: MutableList<AudioTrack> = jsonBrowser
            .runCatching { extractSearchPage(this, trackFactory) }
            .onFailure { throw RuntimeException(it) }
            .getOrThrow()

        return if (tracks.isEmpty()) {
            AudioReference.NO_TRACK
        } else {
            BasicAudioTrackCollection(
                "Search results for: $query",
                AudioTrackCollectionType.SearchResult(query),
                tracks,
                null
            )
        }
    }

    @Throws(IOException::class)
    private fun extractSearchPage(
        jsonBrowser: JsonBrowser,
        trackFactory: AudioTrackFactory
    ): MutableList<AudioTrack> {
        return jsonBrowser["contents"]["sectionListRenderer"]["contents"][0]["itemSectionRenderer"]["contents"]
            .values()
            .mapNotNull { extractPolymerData(it, trackFactory) }
            .toMutableList()
    }

    private fun extractPolymerData(json: JsonBrowser, trackFactory: AudioTrackFactory): AudioTrack? {
        val renderer = json["compactVideoRenderer"].takeUnless { it.isNull }
            ?: return null // Ignore everything which is not a track

        /* Ignore if the video is a live stream */
        if (renderer["lengthText"].isNull) {
            return null
        }

        val videoId = renderer["videoId"].text

        /* create the audio track. */
        val info = AudioTrackInfo(
            title = renderer["title"]["runs"][0]["text"].safeText,
            author = renderer["longBylineText"]["runs"][0]["text"].safeText,
            length = durationTextToMillis(renderer["lengthText"]["runs"][0]["text"].safeText),
            identifier = videoId!!,
            uri = "${YoutubeConstants.WATCH_URL_PREFIX}$videoId",
            artworkUrl = ThumbnailTools.extractYouTube(renderer, videoId)
        )

        return trackFactory.create(info)
    }
}
