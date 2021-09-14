package lavaplayer.source.youtube

import lavaplayer.source.common.Extractor
import lavaplayer.source.common.ExtractorContext
import lavaplayer.source.common.LinkRouter
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.extensions.isLink
import lavaplayer.track.AudioItem
import org.apache.http.client.utils.URIBuilder
import java.net.URISyntaxException

class DefaultYoutubeLinkRouter : LinkRouter.UsingExtractors<YoutubeLinkRoutes> {
    companion object {
        private const val SEARCH_PREFIX = "ytsearch:"
        private const val SEARCH_MUSIC_PREFIX = "ytmsearch:"
        private const val PROTOCOL_REGEX = "(?:http://|https://|)"
        private const val DOMAIN_REGEX = "(?:www\\.|m\\.|music\\.|)youtube\\.com"
        private const val SHORT_DOMAIN_REGEX = "(?:www\\.|)youtu\\.be"
        private const val VIDEO_ID_REGEX = "(?<v>[a-zA-Z0-9_-]{11})"
        private const val PLAYLIST_ID_REGEX = "(?<list>(PL|LL|FL|UU)[a-zA-Z0-9_-]+)"

        private val directVideoIdPattern = "^$VIDEO_ID_REGEX$".toPattern()

        private fun getUrlInfo(url: String, retryValidPart: Boolean): UrlInfo {
            val url = if (!url.isLink()) "https://$url" else url
            return try {
                val builder = URIBuilder(url)
                UrlInfo(builder.path, builder.queryParams
                    .filterNot { it.value == null }
                    .associate { it.name to it.value })
            } catch (e: URISyntaxException) {
                if (retryValidPart) {
                    getUrlInfo(url.substring(0, e.index - 1), false)
                } else {
                    throw FriendlyException("Not a valid URL: $url", FriendlyException.Severity.COMMON, e)
                }
            }
        }
    }

    override val extractors = listOf(
        Extractor("^$SEARCH_PREFIX.+".toPattern(), ::routeSearch),
        Extractor("^$SEARCH_MUSIC_PREFIX.+".toPattern(), ::routeMusicSearch),
        Extractor(directVideoIdPattern, ::routeDirectTrack),
        Extractor("^$PLAYLIST_ID_REGEX$".toPattern(), ::routeDirectPlaylist),
        Extractor("^$PROTOCOL_REGEX$DOMAIN_REGEX/.*".toPattern(), ::routeFromMainDomain),
        Extractor("^$PROTOCOL_REGEX$SHORT_DOMAIN_REGEX/.*".toPattern(), ::routeFromShortDomain),
        Extractor("^$PROTOCOL_REGEX$DOMAIN_REGEX/embed/.*".toPattern(), ::routeFromEmbed),
        Extractor("^$PROTOCOL_REGEX$DOMAIN_REGEX/shorts/.*".toPattern(), ::routeFromShorts)
    )

    private suspend fun routeFromMainDomain(routes: YoutubeLinkRoutes, context: ExtractorContext): AudioItem? {
        val urlInfo = getUrlInfo(context.identifier, true)
        when (urlInfo.path) {
            "/watch" -> {
                val videoId = urlInfo.parameters["v"]
                if (videoId != null) {
                    return routeFromUrlWithVideoId(routes, videoId, urlInfo)
                }
            }

            "/playlist" -> {
                val playlistId = urlInfo.parameters["list"]
                if (playlistId != null) {
                    return routes.playlist(playlistId, null)
                }
            }

            "/watch_videos" -> {
                val videoIds = urlInfo.parameters["video_ids"]
                if (videoIds != null) {
                    return routes.anonymous(videoIds)
                }
            }
        }

        return null
    }

    private suspend fun routeSearch(routes: YoutubeLinkRoutes, context: ExtractorContext): AudioItem? {
        val query = context.identifier
            .drop(SEARCH_PREFIX.length)
            .trim()

        return routes.search(query)
    }

    private suspend fun routeMusicSearch(routes: YoutubeLinkRoutes, context: ExtractorContext): AudioItem? {
        val query = context.identifier
            .drop(SEARCH_PREFIX.length)
            .trim()

        return routes.searchMusic(query)
    }

    private suspend fun routeDirectTrack(routes: YoutubeLinkRoutes, context: ExtractorContext): AudioItem? {
        return routes.track(context.identifier)
    }

    private suspend fun routeDirectPlaylist(routes: YoutubeLinkRoutes, context: ExtractorContext): AudioItem? {
        return routes.playlist(context.identifier, null)
    }

    private suspend fun routeFromShortDomain(routes: YoutubeLinkRoutes, context: ExtractorContext): AudioItem? {
        val urlInfo = getUrlInfo(context.identifier, true)
        return routeFromUrlWithVideoId(routes, urlInfo.path.substring(1), urlInfo)
    }

    private suspend fun routeFromEmbed(routes: YoutubeLinkRoutes, context: ExtractorContext): AudioItem? {
        val urlInfo = getUrlInfo(context.identifier, true)
        return routeFromUrlWithVideoId(routes, urlInfo.path.substring(7), urlInfo)
    }

    private suspend fun routeFromShorts(routes: YoutubeLinkRoutes, context: ExtractorContext): AudioItem? {
        val urlInfo = getUrlInfo(context.identifier, true)
        return routeFromUrlWithVideoId(routes, urlInfo.path.substring(8), urlInfo)
    }

    private suspend fun routeFromUrlWithVideoId(
        routes: YoutubeLinkRoutes,
        identifier: String,
        urlInfo: UrlInfo
    ): AudioItem? {
        val videoId = identifier.take(11)
        return if (!directVideoIdPattern.matcher(videoId).matches()) {
            routes.none()
        } else if (urlInfo.parameters.containsKey("list")) {
            val playlistId: String = urlInfo.parameters["list"]!!
            if (playlistId.startsWith("RD")) {
                routes.mix(playlistId, videoId)
            } else {
                routes.playlist(urlInfo.parameters["list"]!!, videoId)
            }
        } else {
            routes.track(videoId)
        }
    }

    class UrlInfo(val path: String, val parameters: Map<String, String>)
}
