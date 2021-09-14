package lavaplayer.source.youtube

import lavaplayer.source.ItemSourceManager
import lavaplayer.source.common.LinkRouter
import lavaplayer.source.youtube.music.YoutubeMixLoader
import lavaplayer.source.youtube.music.YoutubeMixProvider
import lavaplayer.source.youtube.music.YoutubeSearchMusicProvider
import lavaplayer.source.youtube.music.YoutubeSearchMusicResultLoader
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.tools.http.MultiHttpConfigurable
import lavaplayer.tools.io.*
import lavaplayer.track.*
import lavaplayer.track.loader.LoaderState
import mu.KotlinLogging
import org.apache.http.client.methods.HttpGet
import java.io.DataInput

/**
 * Audio source manager that implements finding YouTube videos or playlists based on a URL or ID.
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
    private val linkRouter: LinkRouter<YoutubeLinkRoutes> = DefaultYoutubeLinkRouter(),
    private val mixLoader: YoutubeMixLoader = YoutubeMixProvider()
) : ItemSourceManager, HttpConfigurable {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val loadingRoutes: LoadingRoutes = LoadingRoutes()
    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()
    private val httpConfiguration = MultiHttpConfigurable(
        listOf(
            httpInterfaceManager,
            searchResultLoader.httpConfiguration,
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
        get() = searchResultLoader.httpConfiguration

    val searchMusicHttpConfiguration: ExtendedHttpConfigurable
        get() = searchMusicResultLoader.getHttpConfiguration()

    /**
     * Maximum number of pages loaded from one playlist. There are 100 tracks per page.
     */
    var playlistPageCount: Int
        get() = playlistLoader.playlistPageCount
        set(value) {
            playlistLoader.playlistPageCount = value
        }

    override val sourceName: String
        get() = "youtube"

    init {
        httpInterfaceManager.setHttpContextFilter(YoutubeHttpContextFilter())
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

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        return YoutubeAudioTrack(trackInfo, this)
    }

    override fun shutdown() {
        ExceptionTools.closeWithWarnings(httpInterfaceManager)
    }

    override fun configureRequests(configurator: RequestConfigurator) {
        httpConfiguration.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: BuilderConfigurator) {
        httpConfiguration.configureBuilder(configurator)
    }

    private suspend fun loadItemOnce(reference: AudioReference): AudioItem? {
        return linkRouter.find(reference.identifier!!, loadingRoutes)
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
            throw ExceptionTools.wrapUnfriendlyException(
                "Loading information for a YouTube track failed.",
                FriendlyException.Severity.FAULT,
                e
            )
        }
    }

    /**
     * Loads a track collection from a mix or playlist id
     *
     * @param type The type of playlist to load.
     * @param id The playlist id.
     * @param selectedVideoId The selected video id.
     */
    fun loadPlaylistWithId(type: PlaylistType, id: String, selectedVideoId: String?): AudioTrackCollection {
        log.debug { "Starting to load ${type.name.lowercase()} with ID $id selected track $selectedVideoId" }
        try {
            httpInterface.use { httpInterface ->
                return mixLoader.load(httpInterface, id, selectedVideoId) { buildTrackFromInfo(it) }
            }
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyException(e)
        }
    }

    private fun buildTrackFromInfo(info: AudioTrackInfo): YoutubeAudioTrack {
        return YoutubeAudioTrack(info, this)
    }

    private inner class LoadingRoutes : YoutubeLinkRoutes {
        override fun track(videoId: String): AudioItem {
            return loadTrackWithVideoId(videoId, false)
        }

        override fun playlist(playlistId: String, selectedVideoId: String?): AudioItem {
            return loadPlaylistWithId(PlaylistType.Regular, playlistId, selectedVideoId)
        }

        override fun mix(mixId: String, selectedVideoId: String?): AudioItem {
            return loadPlaylistWithId(PlaylistType.Mix, mixId, selectedVideoId)
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
                            // YouTube currently transforms watch_video links into a link with a video id and a list id.
                            // because that's what happens, we can simply re-process with the redirected link
                            val redirects = context.redirectLocations
                            return if (redirects != null && redirects.isNotEmpty()) {
                                AudioReference(redirects[0].toString(), null)
                            } else {
                                throw FriendlyException(
                                    "Unable to process YouTube watch_videos link",
                                    FriendlyException.Severity.SUSPICIOUS,
                                    IllegalStateException("Expected YouTube to redirect watch_videos link to a watch?v={id}&list={list_id} link, but it did not redirect at all")
                                )
                            }
                        }
                }
            } catch (e: Exception) {
                throw ExceptionTools.wrapUnfriendlyException(e)
            }
        }

        override fun none(): AudioItem {
            return AudioReference.NO_TRACK
        }
    }

    enum class PlaylistType { Mix, Regular }
}
