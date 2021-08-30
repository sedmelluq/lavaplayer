package lavaplayer.source.vimeo

import lavaplayer.source.ItemSourceManager
import lavaplayer.tools.DataFormatTools
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.JsonBrowser
import lavaplayer.tools.JsonBrowser.Companion.parse
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpConfigurable
import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioReference
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
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
 * Audio source manager which detects Vimeo tracks by URL.
 */
class VimeoItemSourceManager : ItemSourceManager, HttpConfigurable {
    companion object {
        private const val TRACK_URL_REGEX = "^https://vimeo.com/[0-9]+(?:\\?.*|)$"
        private val trackUrlPattern = Pattern.compile(TRACK_URL_REGEX)
    }

    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()

    /**
     * Get an HTTP interface for a playing track.
     */
    val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()

    override val sourceName: String
        get() = "vimeo"

    override suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem? {
        if (!trackUrlPattern.matcher(reference.identifier).matches()) {
            return null
        }

        try {
            httpInterface.use { httpInterface -> return loadFromTrackPage(httpInterface, reference.identifier) }
        } catch (e: IOException) {
            throw FriendlyException("Loading Vimeo track information failed.", FriendlyException.Severity.SUSPICIOUS, e)
        }
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean =
        true

    @Throws(IOException::class)
    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        // Nothing special to encode
    }

    @Throws(IOException::class)
    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        return VimeoAudioTrack(trackInfo, this)
    }

    override fun shutdown() {
        ExceptionTools.closeWithWarnings(httpInterfaceManager)
    }

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    @Throws(IOException::class)
    fun loadConfigJsonFromPageContent(content: String): JsonBrowser? {
        val configText = DataFormatTools.extractBetween(content, "window.vimeo.clip_page_config = ", "\n")
        return if (configText != null) parse(configText) else null
    }

    @Throws(IOException::class)
    private fun loadFromTrackPage(httpInterface: HttpInterface, trackUrl: String?): AudioItem {
        httpInterface.execute(HttpGet(trackUrl)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return AudioReference.NO_TRACK
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw FriendlyException(
                    "Server responded with an error.", FriendlyException.Severity.SUSPICIOUS,
                    IllegalStateException("Response code is $statusCode")
                )
            }

            return loadTrackFromPageContent(trackUrl, IOUtils.toString(response.entity.content, StandardCharsets.UTF_8))
        }
    }

    @Throws(IOException::class)
    private fun loadTrackFromPageContent(trackUrl: String?, content: String): AudioTrack {
        val config = loadConfigJsonFromPageContent(content)
            ?: throw FriendlyException(
                "Track information not found on the page.",
                FriendlyException.Severity.SUSPICIOUS,
                null
            )

        return VimeoAudioTrack(
            AudioTrackInfo(
                config["clip"]["title"].text!!,
                config["owner"]["display_name"].text!!,
                (config["clip"]["duration"]["raw"].asDouble() * 1000.0).toLong(),
                trackUrl!!,
                false,
                trackUrl,
                config["thumbnail"]["src"].text
            ), this
        )
    }

}
