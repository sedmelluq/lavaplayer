package com.sedmelluq.discord.lavaplayer.source.youtube

import kotlinx.serialization.Serializable
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.extensions.decodeJson
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.track.*
import mu.KotlinLogging
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import java.io.IOException

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
                    val searchResults = response.entity.content.decodeJson<SearchResult>()
                    return extractSearchResults(searchResults, query, trackFactory)
                }
            }
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyException(e)
        }
    }

    private fun extractSearchResults(
        searchResults: SearchResult,
        query: String,
        trackFactory: AudioTrackFactory
    ): AudioItem {
        log.debug { "Attempting to parse results from search page" }

        val tracks: MutableList<AudioTrack> = extractSearchPage(searchResults, trackFactory)
            .ifEmpty { return AudioReference.NO_TRACK }

        return BasicAudioTrackCollection(
            "Search results for: $query",
            AudioTrackCollectionType.SearchResult(query),
            tracks,
            null
        )
    }

    @Throws(IOException::class)
    private fun extractSearchPage(searchResults: SearchResult, trackFactory: AudioTrackFactory): MutableList<AudioTrack> {
        return searchResults.contents.sectionListRenderer.contents.first().itemSectionRenderer.contents
            .mapNotNull { extractPolymerData(it, trackFactory) }
            .toMutableList()
    }

    private fun extractPolymerData(listedTrack: SectionList.Track, trackFactory: AudioTrackFactory): AudioTrack? {
        val video = listedTrack.compactVideoRenderer
            ?: return null // Ignore everything which is not a track

        /* Ignore if the video is a live stream */
        val length = video.length
            ?: return null

        /* create the audio track. */
        val info = AudioTrackInfo(
            title = video.title,
            author = video.author!!,
            length = length,
            identifier = video.id,
            uri = "${YoutubeConstants.WATCH_URL_PREFIX}${video.id}",
            artworkUrl = video.thumbnail
        )

        return trackFactory.create(info)
    }

    @Serializable
    data class SearchResult(val contents: Contents) {
        @Serializable
        data class Contents(val sectionListRenderer: SectionList)
    }

    @Serializable
    data class SectionList(val contents: List<Item>) {
        @Serializable
        data class Item(val itemSectionRenderer: Renderer) {
            @Serializable
            data class Renderer(val contents: List<Track>)
        }

        @Serializable
        data class Track(val compactVideoRenderer: YouTubeVideoModel? = null)
    }
}
