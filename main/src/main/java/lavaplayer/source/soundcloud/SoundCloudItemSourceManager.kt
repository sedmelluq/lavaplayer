package lavaplayer.source.soundcloud

import lavaplayer.source.ItemSourceManager
import lavaplayer.source.TrackCollectionLoader
import lavaplayer.source.soundcloud.SoundCloudHelper.nonMobileUrl
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.JsonBrowser
import lavaplayer.tools.JsonBrowser.Companion.parse
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpConfigurable
import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.*
import lavaplayer.track.loader.LoaderState
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern

/**
 * Audio source manager that implements finding SoundCloud tracks based on URL.
 *
 * @param allowSearch Whether to allow search queries as identifiers
 */
class SoundCloudItemSourceManager(
    private val allowSearch: Boolean,
    private val dataReader: SoundCloudDataReader,
    private val htmlDataLoader: SoundCloudHtmlDataLoader,
    val formatHandler: SoundCloudFormatHandler,
    private val playlistLoader: TrackCollectionLoader
) : ItemSourceManager, HttpConfigurable {
    companion object {
        private const val DEFAULT_SEARCH_RESULTS = 10
        private const val MAXIMUM_SEARCH_RESULTS = 200
        private const val TRACK_URL_REGEX =
            "^(?:https?://)?(?:www\\.)?(?:m\\.)?soundcloud\\.com/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)/?(?:\\?.*|)$"
        private const val GOOGLE_TRACK_URL_REGEX =
            "^(?:https?://)?(?:www\\.)?(?:m\\.)?soundcloud\\.app\\.goo\\.gl/([a-zA-Z0-9-_]+)$"
        private const val UNLISTED_URL_REGEX =
            "^(?:http://|https://|)(?:www\\.|)(?:m\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)/s-([a-zA-Z0-9-_]+)(?:\\?.*|)$"
        private const val LIKED_URL_REGEX =
            "^(?:https?://)?(?:www\\.)?(?:m\\.)?soundcloud\\.com/([a-zA-Z0-9-_]+)/likes/?(?:\\?.*|)$"
        private const val LIKED_USER_URN_REGEX = "\"urn\":\"soundcloud:users:([0-9]+)\",\"username\":\"([^\"]+)\""
        private const val SEARCH_PREFIX = "scsearch"
        private const val SEARCH_PREFIX_DEFAULT = "scsearch:"
        private const val SEARCH_REGEX = SEARCH_PREFIX + "\\[([0-9]{1,9}),([0-9]{1,9})\\]:\\s*(.*)\\s*"

        private val searchPattern = Pattern.compile(SEARCH_REGEX)
        private val trackUrlPattern = Pattern.compile(TRACK_URL_REGEX)
        private val googleTrackUrlPattern = Pattern.compile(GOOGLE_TRACK_URL_REGEX)
        private val unlistedUrlPattern = Pattern.compile(UNLISTED_URL_REGEX)
        private val likedUrlPattern = Pattern.compile(LIKED_URL_REGEX)
        private val likedUserUrnPattern = Pattern.compile(LIKED_USER_URN_REGEX)

        @JvmStatic
        fun createDefault(): SoundCloudItemSourceManager {
            val dataReader: SoundCloudDataReader = DefaultSoundCloudDataReader()
            val htmlDataLoader: SoundCloudHtmlDataLoader = DefaultSoundCloudHtmlDataLoader()
            val formatHandler: SoundCloudFormatHandler = DefaultSoundCloudFormatHandler()
            return SoundCloudItemSourceManager(
                true, dataReader, htmlDataLoader, formatHandler,
                DefaultSoundCloudPlaylistLoader(htmlDataLoader, dataReader, formatHandler)
            )
        }

        fun builder(): Builder {
            return Builder()
        }
    }

    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()
    private val clientIdTracker = SoundCloudClientIdTracker(httpInterfaceManager)

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

    fun loadFromTrackPage(trackWebUrl: String?): AudioTrack {
        try {
            httpInterface.use { httpInterface ->
                val rootData = htmlDataLoader.load(httpInterface, trackWebUrl!!)
                val trackData = dataReader.findTrackData(rootData)
                    ?: throw FriendlyException("This track is not available", FriendlyException.Severity.COMMON, null)

                return loadFromTrackData(trackData)
            }
        } catch (e: IOException) {
            throw FriendlyException("Loading track from SoundCloud failed.", FriendlyException.Severity.SUSPICIOUS, e)
        }
    }

    override suspend fun loadItem(state: LoaderState, reference: AudioReference) = processAsSingleTrack(reference)
        ?: playlistLoader.load(reference.identifier!!, httpInterfaceManager) { buildTrackFromInfo(it) }
        ?: processAsLikedTracks(reference)
        ?: allowSearch.takeIf { it }?.let { processAsSearchQuery(reference) }

    override fun isTrackEncodable(track: AudioTrack): Boolean =
        true

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack =
        SoundCloudAudioTrack(trackInfo, this)

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        // No extra information to save
    }

    override fun shutdown() {
        // Nothing to shut down
    }

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    private fun processAsSingleTrack(reference: AudioReference): AudioTrack? {
        val url = nonMobileUrl(reference.identifier!!)
        val trackUrlMatcher = trackUrlPattern.matcher(url)
        if (trackUrlMatcher.matches() && "likes" != trackUrlMatcher.group(2)) {
            return loadFromTrackPage(url)
        }

        val googleTrackUrlMatcher = googleTrackUrlPattern.matcher(url)
        if (googleTrackUrlMatcher.matches()) {
            return loadFromTrackPage(url)
        }

        val unlistedUrlMatcher = unlistedUrlPattern.matcher(url)
        return if (unlistedUrlMatcher.matches()) loadFromTrackPage(url) else null
    }

    private fun processAsLikedTracks(reference: AudioReference): AudioItem? {
        val url = nonMobileUrl(reference.identifier!!)
        return if (likedUrlPattern.matcher(url).matches()) {
            loadFromLikedTracks(url)
        } else {
            null
        }
    }

    private fun loadFromTrackData(trackData: JsonBrowser?): AudioTrack {
        val format = formatHandler.chooseBestFormat(dataReader.readTrackFormats(trackData!!))
        return buildTrackFromInfo(dataReader.readTrackInfo(trackData, formatHandler.buildFormatIdentifier(format)))
    }

    private fun buildTrackFromInfo(trackInfo: AudioTrackInfo): AudioTrack =
        SoundCloudAudioTrack(trackInfo, this)

    private fun loadFromLikedTracks(likedListUrl: String): AudioItem {
        try {
            httpInterface.use { httpInterface ->
                val userInfo = findUserIdFromLikedList(httpInterface, likedListUrl) ?: return AudioReference.NO_TRACK
                return extractTracksFromLikedList(loadLikedListForUserId(httpInterface, userInfo), userInfo)
            }
        } catch (e: IOException) {
            throw FriendlyException(
                "Loading liked tracks from SoundCloud failed.",
                FriendlyException.Severity.SUSPICIOUS,
                e
            )
        }
    }

    @Throws(IOException::class)
    private fun findUserIdFromLikedList(httpInterface: HttpInterface, likedListUrl: String): UserInfo? {
        httpInterface.execute(HttpGet(likedListUrl)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return null
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw IOException("Invalid status code for track list response: $statusCode")
            }

            val matcher = likedUserUrnPattern.matcher(IOUtils.toString(response.entity.content, StandardCharsets.UTF_8))
            return if (matcher.find()) UserInfo(matcher.group(1), matcher.group(2)) else null
        }
    }

    @Throws(IOException::class)
    private fun loadLikedListForUserId(httpInterface: HttpInterface, userInfo: UserInfo): JsonBrowser {
        val uri = URI.create("https://api-v2.soundcloud.com/users/${userInfo.id}/likes?limit=200&offset=0")
        httpInterface.execute(HttpGet(uri)).use { response ->
            HttpClientTools.assertSuccessWithContent(response, "liked tracks response")
            return parse(response.entity.content)
        }
    }

    private fun extractTracksFromLikedList(likedTracks: JsonBrowser, userInfo: UserInfo): AudioItem {
        val tracks: MutableList<AudioTrack> = ArrayList()
        for (item in likedTracks["collection"].values()) {
            val trackItem = item["track"]
            if (!trackItem.isNull && !dataReader.isTrackBlocked(trackItem)) {
                tracks.add(loadFromTrackData(trackItem))
            }
        }

        return BasicAudioTrackCollection(
            "Liked by " + userInfo.name,
            AudioTrackCollectionType.Playlist,
            tracks,
            null
        )
    }

    private fun processAsSearchQuery(reference: AudioReference): AudioItem? {
        if (reference.identifier!!.startsWith(SEARCH_PREFIX)) {
            if (reference.identifier.startsWith(SEARCH_PREFIX_DEFAULT)) {
                return loadSearchResult(
                    reference.identifier.substring(SEARCH_PREFIX_DEFAULT.length).trim { it <= ' ' },
                    0,
                    DEFAULT_SEARCH_RESULTS
                )
            }

            val searchMatcher = searchPattern.matcher(reference.identifier)
            if (searchMatcher.matches()) {
                return loadSearchResult(
                    searchMatcher.group(3),
                    searchMatcher.group(1).toInt(),
                    searchMatcher.group(2).toInt()
                )
            }
        }

        return null
    }

    private fun loadSearchResult(query: String, offset: Int, rawLimit: Int): AudioItem {
        val limit = rawLimit.coerceAtMost(MAXIMUM_SEARCH_RESULTS)
        try {
            httpInterface.use { httpInterface ->
                httpInterface
                    .execute(HttpGet(buildSearchUri(query, offset, limit)))
                    .use { return loadSearchResultsFromResponse(it, query) }
            }
        } catch (e: IOException) {
            throw FriendlyException(
                "Loading search results from SoundCloud failed.",
                FriendlyException.Severity.SUSPICIOUS,
                e
            )
        }
    }

    @Throws(IOException::class)
    private fun loadSearchResultsFromResponse(response: HttpResponse, query: String): AudioItem {
        return try {
            val searchResults = parse(response.entity.content)
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

    private fun extractTracksFromSearchResults(query: String, searchResults: JsonBrowser): AudioItem {
        val tracks: MutableList<AudioTrack> = ArrayList()
        for (item in searchResults["collection"].values()) {
            if (!item.isNull) {
                tracks.add(loadFromTrackData(item))
            }
        }
        return BasicAudioTrackCollection(
            "Search results for: $query",
            AudioTrackCollectionType.SearchResult(query),
            tracks,
            null
        )
    }

    private data class UserInfo(val id: String, val name: String)

    class Builder {
        var allowSearch = true

        var dataReader: SoundCloudDataReader? = null

        var htmlDataLoader: SoundCloudHtmlDataLoader? = null

        var formatHandler: SoundCloudFormatHandler? = null

        var playlistLoader: TrackCollectionLoader? = null

        var playlistLoaderFactory: PlaylistLoaderFactory? = null

        fun build(): SoundCloudItemSourceManager {
            val usedDataReader = dataReader
                ?: DefaultSoundCloudDataReader()

            val usedHtmlDataLoader = htmlDataLoader
                ?: DefaultSoundCloudHtmlDataLoader()

            val usedFormatHandler = formatHandler
                ?: DefaultSoundCloudFormatHandler()

            val usedPlaylistLoader = playlistLoader
                ?: playlistLoaderFactory?.create(usedDataReader, usedHtmlDataLoader, usedFormatHandler)
                ?: DefaultSoundCloudPlaylistLoader(usedHtmlDataLoader, usedDataReader, usedFormatHandler)

            return SoundCloudItemSourceManager(
                allowSearch,
                usedDataReader,
                usedHtmlDataLoader,
                usedFormatHandler,
                usedPlaylistLoader
            )
        }

        fun interface PlaylistLoaderFactory {
            fun create(
                dataReader: SoundCloudDataReader,
                htmlDataLoader: SoundCloudHtmlDataLoader,
                formatHandler: SoundCloudFormatHandler
            ): TrackCollectionLoader
        }
    }
}
