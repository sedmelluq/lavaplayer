package lavaplayer.source.nico

import lavaplayer.source.ItemSourceManager
import lavaplayer.tools.DataFormatTools
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.extensions.UrlEncodedFormEntity
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpConfigurable
import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioReference
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.loading.ItemLoader
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClientBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern

/**
 * Audio source manager that implements finding NicoNico tracks based on URL.
 */
class NicoItemSourceManager(private val email: String, private val password: String) : ItemSourceManager, HttpConfigurable {
    companion object {
        private const val TRACK_URL_REGEX = """^https?://(?:www\.)?nicovideo\.jp/watch/(sm[0-9]+)(?:\?.*)?$"""
        private val trackUrlPattern = Pattern.compile(TRACK_URL_REGEX)
        private fun getWatchUrl(videoId: String): String = "http://www.nicovideo.jp/watch/$videoId"
    }

    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()
    private val loggedIn = AtomicBoolean()

    /**
     * @return Get an HTTP interface for a playing track.
     */
    val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()

    fun checkLoggedIn() = synchronized(loggedIn) {
        if (loggedIn.get()) {
            return
        }

        val loginRequest = HttpPost("https://secure.nicovideo.jp/secure/login")
        loginRequest.entity = UrlEncodedFormEntity("mail" to email, "password" to password, charset = Charsets.UTF_8)

        try {
            httpInterface.use { httpInterface ->
                httpInterface.execute(loginRequest).use { response ->
                    val statusCode = response.statusLine.statusCode.takeUnless { it != 302 }
                    if (statusCode != 302) {
                        throw IOException("Unexpected response code $statusCode")
                    }

                    val location = response.getFirstHeader("Location")
                    if (location == null || location.value.contains("message=")) {
                        throw FriendlyException("Login details for NicoNico are invalid.", FriendlyException.Severity.COMMON, null)
                    }

                    loggedIn.set(true)
                }
            }
        } catch (e: IOException) {
            throw FriendlyException("Exception when trying to log into NicoNico", FriendlyException.Severity.SUSPICIOUS, e)
        }
    }

    override fun getSourceName(): String =
        "niconico"

    override fun isTrackEncodable(track: AudioTrack): Boolean =
        true

    @Throws(IOException::class)
    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack =
        NicoAudioTrack(trackInfo, this)

    override fun loadItem(itemLoader: ItemLoader, reference: AudioReference): AudioItem? {
        val trackMatcher = trackUrlPattern.matcher(reference.identifier)
        return if (trackMatcher.matches()) loadTrack(trackMatcher.group(1)) else null
    }

    @Throws(IOException::class)
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

    private fun loadTrack(videoId: String): AudioTrack {
        checkLoggedIn()
        try {
            httpInterface.use { httpInterface ->
                httpInterface.execute(HttpGet("http://ext.nicovideo.jp/api/getthumbinfo/$videoId")).use { response ->
                    val statusCode = response.statusLine.statusCode
                    if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                        throw IOException("Unexpected response code from video info: $statusCode")
                    }

                    val document =
                        Jsoup.parse(response.entity.content, StandardCharsets.UTF_8.name(), "", Parser.xmlParser())

                    return extractTrackFromXml(videoId, document)!!
                }
            }
        } catch (e: IOException) {
            throw FriendlyException("Error occurred when extracting video info.", FriendlyException.Severity.SUSPICIOUS, e)
        }
    }

    private fun extractTrackFromXml(videoId: String, document: Document): AudioTrack? {
        for (element in document.select(":root > thumb")) {
            val uploader = element.select("user_nickname").first()!!.text()
            val title = element.select("title").first()!!.text()
            val artworkUrl = element.select("thumbnail_url").first()!!.text()
            val duration = DataFormatTools.durationTextToMillis(element.select("length").first()!!.text())

            val trackInfo = AudioTrackInfo(
                title,
                uploader,
                duration,
                videoId,
                false,
                getWatchUrl(videoId),
                artworkUrl
            )

            return NicoAudioTrack(trackInfo, this)
        }

        return null
    }
}
