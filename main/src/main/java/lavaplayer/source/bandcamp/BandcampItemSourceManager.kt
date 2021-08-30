package lavaplayer.source.bandcamp

import lavaplayer.source.ItemSourceManager
import lavaplayer.tools.DataFormatTools.extractBetween
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.JsonBrowser
import lavaplayer.tools.JsonBrowser.Companion.parse
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpConfigurable
import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.io.HttpInterfaceManager
import lavaplayer.track.*
import lavaplayer.track.AudioTrackCollectionType.Album
import lavaplayer.track.loader.LoaderState
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern

/**
 * Audio source manager that implements finding Bandcamp tracks based on URL.
 */
class BandcampItemSourceManager : ItemSourceManager, HttpConfigurable {
    private val httpInterfaceManager: HttpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()

    override val sourceName: String
        get() = "bandcamp"

    override suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem? {
        val urlInfo = parseUrl(reference.identifier)
        return if (urlInfo != null) {
            if (urlInfo.isAlbum) {
                loadAlbum(urlInfo)
            } else {
                loadTrack(urlInfo)
            }
        } else {
            null
        }
    }

    private fun parseUrl(url: String?): UrlInfo? {
        val matcher = urlRegex.matcher(url)
        return if (matcher.matches()) {
            UrlInfo(url, matcher.group(1), "album" == matcher.group(2))
        } else {
            null
        }
    }

    private fun loadTrack(urlInfo: UrlInfo): AudioItem? {
        return extractFromPage(urlInfo.fullUrl) { _: HttpInterface?, text: String? ->
            val trackListInfo = readTrackListInformation(text)
            val artist = trackListInfo["artist"].safeText
            val artworkUrl = extractArtwork(trackListInfo)
            extractTrack(trackListInfo["trackinfo"].index(0), urlInfo.baseUrl, artist, artworkUrl)
        }
    }

    private fun loadAlbum(urlInfo: UrlInfo): AudioItem? {
        return extractFromPage(urlInfo.fullUrl) { _: HttpInterface?, text: String? ->
            val trackListInfo = readTrackListInformation(text)
            val artist = trackListInfo["artist"].safeText
            val artworkUrl = extractArtwork(trackListInfo)
            val tracks: MutableList<AudioTrack> = ArrayList()
            for (trackInfo in trackListInfo["trackinfo"].values()) {
                tracks.add(extractTrack(trackInfo, urlInfo.baseUrl, artist, artworkUrl))
            }

            val albumInfo = readAlbumInformation(text)
            BasicAudioTrackCollection(
                albumInfo["current"]["title"].safeText,
                Album(artist),
                tracks,
                null
            )
        }
    }

    private fun extractTrack(trackInfo: JsonBrowser, bandUrl: String, artist: String, artworkUrl: String?): AudioTrack {
        val trackPageUrl = bandUrl + trackInfo["title_link"].text
        return BandcampAudioTrack(
            AudioTrackInfo(
                trackInfo["title"].text!!,
                artist, (trackInfo["duration"].asDouble() * 1000.0).toLong(),
                bandUrl + trackInfo["title_link"].text,
                false,
                trackPageUrl,
                artworkUrl
            ), this
        )
    }

    @Throws(IOException::class)
    private fun readAlbumInformation(text: String?): JsonBrowser {
        var albumInfoJson = extractBetween(text!!, "data-tralbum=\"", "\"")
            ?: throw FriendlyException(
                "Album information not found on the Bandcamp page.",
                FriendlyException.Severity.SUSPICIOUS,
                null
            )
        albumInfoJson = albumInfoJson.replace("&quot;", "\"")
        return parse(albumInfoJson)
    }

    @Throws(IOException::class)
    fun readTrackListInformation(text: String?): JsonBrowser {
        var trackInfoJson = extractBetween(text!!, "data-tralbum=\"", "\"")
            ?: throw FriendlyException(
                "Track information not found on the Bandcamp page.",
                FriendlyException.Severity.SUSPICIOUS,
                null
            )

        trackInfoJson = trackInfoJson.replace("&quot;", "\"")
        return parse(trackInfoJson)
    }

    private fun extractFromPage(url: String?, extractor: AudioItemExtractor): AudioItem? {
        try {
            httpInterfaceManager
                .get()
                .use { httpInterface ->
                    return extractFromPageWithInterface(httpInterface, url, extractor)
                }
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyExceptions(
                "Loading information for a Bandcamp track failed.",
                FriendlyException.Severity.FAULT,
                e
            )
        }
    }

    @Throws(Exception::class)
    private fun extractFromPageWithInterface(
        httpInterface: HttpInterface,
        url: String?,
        extractor: AudioItemExtractor
    ): AudioItem? {
        var responseText: String?
        httpInterface.execute(HttpGet(url)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return AudioReference(null, null)
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw IOException("Invalid status code for track page: $statusCode")
            }

            responseText = IOUtils.toString(response.entity.content, StandardCharsets.UTF_8)
        }

        return extractor.extract(httpInterface, responseText)
    }

    private fun extractArtwork(root: JsonBrowser): String? {
        var artId = root["art_id"].text
        if (artId != null) {
            if (artId.length < 10) {
                val builder = StringBuilder(artId)
                while (builder.length < 10) {
                    builder.insert(0, "0")
                }
                artId = builder.toString()
            }
            return String.format(ARTWORK_URL_FORMAT, artId)
        }
        return null
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return true
    }

    @Throws(IOException::class)
    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        // No special values to encode
    }

    @Throws(IOException::class)
    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        return BandcampAudioTrack(trackInfo, this)
    }

    override fun shutdown() {
        ExceptionTools.closeWithWarnings(httpInterfaceManager)
    }

    /**
     * @return Get an HTTP interface for a playing track.
     */
    val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    private fun interface AudioItemExtractor {
        @Throws(Exception::class)
        fun extract(httpInterface: HttpInterface, text: String?): AudioItem?
    }

    private data class UrlInfo(val fullUrl: String?, val baseUrl: String, val isAlbum: Boolean)

    companion object {
        private const val URL_REGEX =
            "^(https?://(?:[^.]+\\.|)bandcamp\\.com)/(track|album)/([a-zA-Z0-9-_]+)/?(?:\\?.*|)$"
        private const val ARTWORK_URL_FORMAT = "https://f4.bcbits.com/img/a%s_9.jpg"
        private val urlRegex = Pattern.compile(URL_REGEX)
    }

}
