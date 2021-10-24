package com.sedmelluq.discord.lavaplayer.source.soundcloud

import com.sedmelluq.discord.lavaplayer.source.ItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.common.LinkRouter
import com.sedmelluq.discord.lavaplayer.source.common.TrackCollectionLoader
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.extensions.decodeJson
import com.sedmelluq.discord.lavaplayer.tools.io.*
import com.sedmelluq.discord.lavaplayer.track.*
import com.sedmelluq.discord.lavaplayer.track.loader.LoaderState
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.util.EntityUtils
import java.io.DataInput
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Pattern

/**
 * Audio source manager that implements finding SoundCloud tracks based on URL.
 *
 * @param allowSearch Whether to allow search queries as identifiers
 */
class SoundCloudItemSourceManager(
    private val allowSearch: Boolean,
    private val htmlDataLoader: SoundCloudHtmlDataLoader,
    val formatHandler: SoundCloudFormatHandler,
    private val playlistLoader: TrackCollectionLoader,
    private val linkRouter: LinkRouter<SoundCloudLinkRoutes> = SoundCloudLinkRouter()
) : ItemSourceManager, HttpConfigurable {
    companion object {
        /* other bs */
        internal const val DEFAULT_SEARCH_RESULTS = 10

        private const val MAXIMUM_SEARCH_RESULTS = 200
        private const val LIKED_USER_URN_REGEX = "\"urn\":\"soundcloud:users:([0-9]+)\",\"username\":\"([^\"]+)\""

        private val likedUserUrnPattern = Pattern.compile(LIKED_USER_URN_REGEX)

        @JvmStatic
        fun createDefault(): SoundCloudItemSourceManager {
            val htmlDataLoader: SoundCloudHtmlDataLoader = DefaultSoundCloudHtmlDataLoader()
            val formatHandler: SoundCloudFormatHandler = DefaultSoundCloudFormatHandler()
            return SoundCloudItemSourceManager(
                true, htmlDataLoader, formatHandler,
                DefaultSoundCloudPlaylistLoader(htmlDataLoader, formatHandler)
            )
        }

        fun builder(): Builder {
            return Builder()
        }
    }

    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()
    private val clientIdTracker = SoundCloudClientIdTracker(httpInterfaceManager)
    private val loadingRoutes = LoadingRoutes()

    val clientId: String
        get() = clientIdTracker.clientId

    /**
     * @return Get an HTTP interface for a playing track.
     */
    val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()

    override val sourceName: String
        get() = "soundcloud"

    init {
        httpInterfaceManager.setHttpContextFilter(SoundCloudHttpContextFilter(clientIdTracker))
    }

    override suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem? {
        return linkRouter.find(reference.identifier!!, loadingRoutes)
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean =
        true

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack =
        SoundCloudAudioTrack(trackInfo, this)

    override fun configureRequests(configurator: RequestConfigurator) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: BuilderConfigurator) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    private fun loadFromTrackData(track: SoundCloudTrackModel): AudioTrack {
        val format = formatHandler.chooseBestFormat(track.trackFormats)
        val info = track.getTrackInfo(formatHandler.buildFormatIdentifier(format))

        return buildTrackFromInfo(info)
    }

    private fun buildTrackFromInfo(trackInfo: AudioTrackInfo): AudioTrack =
        SoundCloudAudioTrack(trackInfo, this)

    @Throws(IOException::class)
    private fun findUserIdFromLikedList(httpInterface: HttpInterface, likedListUrl: String): UserInfo? {
        httpInterface.execute(HttpGet(likedListUrl)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return null
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw IOException("Invalid status code for track list response: $statusCode")
            }

            val responseText = EntityUtils.toString(response.entity, Charsets.UTF_8)
            val matcher = likedUserUrnPattern.matcher(responseText)
            return if (matcher.find()) UserInfo(matcher.group(1), matcher.group(2)) else null
        }
    }

    @Throws(IOException::class)
    private fun loadLikedListForUserId(httpInterface: HttpInterface, userInfo: UserInfo): SoundCloudUserLikedModel {
        val uri = URI.create("https://api-v2.soundcloud.com/users/${userInfo.id}/likes?limit=200&offset=0")
        httpInterface.execute(HttpGet(uri)).use { response ->
            HttpClientTools.assertSuccessWithContent(response, "liked tracks response")
            return response.entity.content.decodeJson()
        }
    }

    private fun extractTracksFromLikedList(liked: SoundCloudUserLikedModel, userInfo: UserInfo): AudioItem {
        val tracks: List<AudioTrack> = liked.collection
            .filterNot { it.track.isBlocked }
            .map { loadFromTrackData(it.track) }

        return BasicAudioTrackCollection(
            "Liked by " + userInfo.name,
            AudioTrackCollectionType.Playlist,
            tracks.toMutableList(),
            null
        )
    }

    @Throws(IOException::class)
    private fun loadSearchResultsFromResponse(response: HttpResponse, query: String): AudioItem {
        return try {
            val searchResults = response.entity.content.decodeJson<SoundCloudSearchResultModel>()
            extractTracksFromSearchResults(query, searchResults)
        } finally {
            EntityUtils.consumeQuietly(response.entity)
        }
    }

    private fun buildSearchUri(query: String, offset: Int, limit: Int): URI {
        return try {
            URIBuilder("https://api-v2.soundcloud.com/search/tracks")
                .addParameter("q", query)
                .addParameter("offset", offset.toString())
                .addParameter("limit", limit.toString())
                .build()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    private fun extractTracksFromSearchResults(query: String, searchResults: SoundCloudSearchResultModel): AudioItem {
        val tracks: MutableList<AudioTrack> = searchResults.collection
            .filter { it.policy.playable }
            .map { loadFromTrackData(it) }
            .toMutableList()

        return BasicAudioTrackCollection(
            "Search results for: $query",
            AudioTrackCollectionType.SearchResult(query),
            tracks,
            null
        )
    }

    internal fun loadFromTrackPage(trackWebUrl: String?): AudioTrack? {
        try {
            httpInterface.use { httpInterface ->
                val rootData = htmlDataLoader.load(httpInterface, trackWebUrl!!)
                    ?: return null

                val trackData = rootData.resources.firstNotNullOfOrNull { it.data as? SoundCloudTrackModel }
                    ?: throw FriendlyException("This track is not available", FriendlyException.Severity.COMMON, null)

                return loadFromTrackData(trackData)
            }
        } catch (e: IOException) {
            throw FriendlyException("Loading track from SoundCloud failed.", FriendlyException.Severity.SUSPICIOUS, e)
        }
    }

    private data class UserInfo(val id: String, val name: String)

    inner class LoadingRoutes : SoundCloudLinkRoutes {
        override fun track(url: String): AudioItem? {
            return loadFromTrackPage(url)
        }

        override fun liked(url: String): AudioItem? {
            val nonMobileUrl = SoundCloudHelper.nonMobileUrl(url)
            try {
                httpInterface.use { httpInterface ->
                    val userInfo = findUserIdFromLikedList(httpInterface, nonMobileUrl)
                        ?: return AudioReference.NO_TRACK

                    return extractTracksFromLikedList(loadLikedListForUserId(httpInterface, userInfo), userInfo)
                }
            } catch (e: IOException) {
                throw FriendlyException("Loading liked tracks from SoundCloud failed.", FriendlyException.Severity.SUSPICIOUS, e)
            }
        }

        override fun set(url: String): AudioItem? {
            val nonMobileUrl = SoundCloudHelper.nonMobileUrl(url)
            return playlistLoader.load(nonMobileUrl, httpInterfaceManager) { buildTrackFromInfo(it) }
        }

        override fun search(query: String, offset: Int, limit: Int): AudioItem {
            val fixedLimit = limit.coerceAtMost(MAXIMUM_SEARCH_RESULTS)
            try {
                httpInterface.use { httpInterface ->
                    httpInterface
                        .execute(HttpGet(buildSearchUri(query, offset, fixedLimit)))
                        .use { return loadSearchResultsFromResponse(it, query) }
                }
            } catch (e: IOException) {
                throw FriendlyException("Loading search results from SoundCloud failed.", FriendlyException.Severity.SUSPICIOUS, e)
            }
        }

    }

    class Builder {
        var allowSearch = true

        var htmlDataLoader: SoundCloudHtmlDataLoader? = null

        var formatHandler: SoundCloudFormatHandler? = null

        var playlistLoader: TrackCollectionLoader? = null

        var playlistLoaderFactory: PlaylistLoaderFactory? = null

        fun build(): SoundCloudItemSourceManager {
            val usedHtmlDataLoader = htmlDataLoader
                ?: DefaultSoundCloudHtmlDataLoader()

            val usedFormatHandler = formatHandler
                ?: DefaultSoundCloudFormatHandler()

            val usedPlaylistLoader = playlistLoader
                ?: playlistLoaderFactory?.create(usedHtmlDataLoader, usedFormatHandler)
                ?: DefaultSoundCloudPlaylistLoader(usedHtmlDataLoader, usedFormatHandler)

            return SoundCloudItemSourceManager(
                allowSearch,
                usedHtmlDataLoader,
                usedFormatHandler,
                usedPlaylistLoader
            )
        }

        fun interface PlaylistLoaderFactory {
            fun create(
                htmlDataLoader: SoundCloudHtmlDataLoader,
                formatHandler: SoundCloudFormatHandler
            ): TrackCollectionLoader
        }
    }
}
