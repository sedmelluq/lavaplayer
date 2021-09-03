package lavaplayer.source.youtube

import lavaplayer.source.ItemSourceManager
import lavaplayer.source.youtube.YoutubeLinkRouter.Routes
import lavaplayer.source.youtube.music.YoutubeMixLoader
import lavaplayer.source.youtube.music.YoutubeMixProvider
import lavaplayer.source.youtube.music.YoutubeSearchMusicProvider
import lavaplayer.source.youtube.music.YoutubeSearchMusicResultLoader
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.tools.http.MultiHttpConfigurable
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpConfigurable
import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioReference
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.loader.LoaderState
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import java.io.DataInput
import java.io.DataOutput
import java.util.function.Consumer
import java.util.function.Function

/**
 * Audio source manager that implements finding Youtube videos or playlists based on an URL or ID.
 *
 * @param allowSearch Whether to allow search queries as identifiers
 */
class YoutubeItemSourceManager @JvmOverloads constructor(
    private val allowSearch: Boolean = true,
    val trackDetailsLoader: YoutubeTrackDetailsLoader = DefaultYoutubeTrackDetailsLoader(),
    private val searchResultLoader: YoutubeSearchResultLoader = YoutubeSearchProvider(),
    private val searchMusicResultLoader: YoutubeSearchMusicResultLoader = YoutubeSearchMusicProvider(),
    val signatureResolver: YoutubeSignatureResolver = YoutubeSignatureCipherManager(),
    private val playlistLoader: YoutubePlaylistLoader = DefaultYoutubePlaylistLoader(),
    private val linkRouter: YoutubeLinkRouter = DefaultYoutubeLinkRouter(),
    private val mixLoader: YoutubeMixLoader = YoutubeMixProvider()
) : ItemSourceManager, HttpConfigurable {
    companion object {
        private val log = LoggerFactory.getLogger(YoutubeItemSourceManager::class.java)
    }

    private val loadingRoutes: LoadingRoutes = LoadingRoutes()
    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()
    private val httpConfiguration = MultiHttpConfigurable(
        listOf(
            httpInterfaceManager,
            searchResultLoader.getHttpConfiguration(),
            searchMusicResultLoader.getHttpConfiguration()
        )
    )

    /**
     * @return Get an HTTP interface for a playing track.
     */
    val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()

    val mainHttpConfiguration: ExtendedHttpConfigurable
        get() = httpInterfaceManager

    val searchHttpConfiguration: ExtendedHttpConfigurable
        get() = searchResultLoader.getHttpConfiguration()

    val searchMusicHttpConfiguration: ExtendedHttpConfigurable
        get() = searchMusicResultLoader.getHttpConfiguration()

    override val sourceName: String
        get() = "youtube"

    init {
        httpInterfaceManager.setHttpContextFilter(YoutubeHttpContextFilter())
    }

    /**
     * @param playlistPageCount Maximum number of pages loaded from one playlist. There are 100 tracks per page.
     */
    fun setPlaylistPageCount(playlistPageCount: Int) {
        playlistLoader.setPlaylistPageCount(playlistPageCount)
    }

    override suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem? {
        return try {
            loadItemOnce(reference)
        } catch (exception: FriendlyException) {
            // In case of a connection reset exception, try once more.
            if (HttpClientTools.isRetriableNetworkException(exception.cause)) {
                loadItemOnce(reference)
            } else {
                throw exception
            }
        }
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return true
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        // No custom values that need saving
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        return YoutubeAudioTrack(trackInfo, this)
    }

    override fun shutdown() {
        ExceptionTools.closeWithWarnings(httpInterfaceManager)
    }

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        httpConfiguration.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        httpConfiguration.configureBuilder(configurator)
    }

    private fun loadItemOnce(reference: AudioReference): AudioItem? {
        return linkRouter.route(reference.identifier!!, loadingRoutes)
    }

    /**
     * Loads a single track from video ID.
     *
     * @param videoId   ID of the YouTube video.
     * @param mustExist True if it should throw an exception on missing track, otherwise returns AudioReference.NO_TRACK.
     * @return Loaded YouTube track.
     */
    fun loadTrackWithVideoId(videoId: String?, mustExist: Boolean): AudioItem {
        try {
            httpInterface.use { httpInterface ->
                val details = trackDetailsLoader.loadDetails(httpInterface, videoId!!, false, this)
                    ?: if (mustExist) {
                        throw FriendlyException("Video unavailable", FriendlyException.Severity.COMMON, null)
                    } else {
                        return AudioReference.NO_TRACK
                    }

                return YoutubeAudioTrack(details.getTrackInfo(), this)
            }
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyExceptions(
                "Loading information for a YouTube track failed.",
                FriendlyException.Severity.FAULT,
                e
            )
        }
    }

    private fun buildTrackFromInfo(info: AudioTrackInfo): YoutubeAudioTrack {
        return YoutubeAudioTrack(info, this)
    }

    private inner class LoadingRoutes : Routes<AudioItem?> {
        override fun track(videoId: String): AudioItem {
            return loadTrackWithVideoId(videoId, false)
        }

        override fun playlist(playlistId: String, selectedVideoId: String?): AudioItem {
            log.debug("Starting to load playlist with ID {}", playlistId)
            try {
                httpInterface.use { httpInterface ->
                    return playlistLoader.load(httpInterface, playlistId, selectedVideoId) { info: AudioTrackInfo ->
                        buildTrackFromInfo(info)
                    }
                }
            } catch (e: Exception) {
                throw ExceptionTools.wrapUnfriendlyExceptions(e)
            }
        }

        override fun mix(mixId: String, selectedVideoId: String?): AudioItem {
            log.debug("Starting to load mix with ID {} selected track {}", mixId, selectedVideoId)
            try {
                httpInterface.use { httpInterface ->
                    return mixLoader.load(httpInterface, mixId, selectedVideoId) { info: AudioTrackInfo ->
                        buildTrackFromInfo(info)
                    }
                }
            } catch (e: Exception) {
                throw ExceptionTools.wrapUnfriendlyExceptions(e)
            }
        }

        override fun search(query: String): AudioItem? {
            if (!allowSearch) {
                return null
            }

            return searchResultLoader.loadSearchResult(query) { buildTrackFromInfo(it) }
        }

        override fun searchMusic(query: String): AudioItem? {
            if (!allowSearch) {
                return null
            }

            return searchMusicResultLoader.loadSearchMusicResult(query) { buildTrackFromInfo(it) }
        }

        override fun anonymous(videoIds: String): AudioItem {
            try {
                httpInterface.use { httpInterface ->
                    httpInterface.execute(HttpGet("https://www.youtube.com/watch_videos?video_ids=$videoIds"))
                        .use { response ->
                            HttpClientTools.assertSuccessWithContent(response, "playlist response")
                            val context = httpInterface.context
                            // youtube currently transforms watch_video links into a link with a video id and a list id.
                            // because thats what happens, we can simply re-process with the redirected link
                            val redirects = context.redirectLocations
                            return if (redirects != null && !redirects.isEmpty()) {
                                AudioReference(redirects[0].toString(), null)
                            } else {
                                throw FriendlyException(
                                    "Unable to process youtube watch_videos link",
                                    FriendlyException.Severity.SUSPICIOUS,
                                    IllegalStateException("Expected youtube to redirect watch_videos link to a watch?v={id}&list={list_id} link, but it did not redirect at all")
                                )
                            }
                        }
                }
            } catch (e: Exception) {
                throw ExceptionTools.wrapUnfriendlyExceptions(e)
            }
        }

        override fun none(): AudioItem {
            return AudioReference.NO_TRACK
        }
    }
}
