package lavaplayer.source.getyarn

import lavaplayer.source.ItemSourceManager
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.io.*
import lavaplayer.tools.io.HttpClientTools.NoRedirectsStrategy
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioReference
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.loader.LoaderState
import org.apache.commons.io.IOUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.jsoup.Jsoup
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern

/**
 * Audio source manager which detects getyarn.io tracks by URL.
 */
class GetyarnItemSourceManager : HttpConfigurable, ItemSourceManager {
    private val httpInterfaceManager: HttpInterfaceManager = ThreadLocalHttpInterfaceManager(
        HttpClientTools
            .createSharedCookiesHttpBuilder()
            .setRedirectStrategy(NoRedirectsStrategy()),
        HttpClientTools.DEFAULT_REQUEST_CONFIG
    )

    val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()

    override val sourceName: String
        get() = "getyarn.io"

    override suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem? {
        val m = GETYARN_REGEX.matcher(reference.identifier)
        return if (!m.matches()) {
            null
        } else {
            extractVideoUrlFromPage(reference)
        }
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return true
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        // No custom values that need saving
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        return GetyarnAudioTrack(trackInfo, this)
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

    private fun createTrack(trackInfo: AudioTrackInfo): AudioTrack {
        return GetyarnAudioTrack(trackInfo, this)
    }

    private fun extractVideoUrlFromPage(reference: AudioReference): AudioTrack {
        try {
            httpInterface.execute(HttpGet(reference.identifier)).use { response ->
                val html = IOUtils.toString(response.entity.content, StandardCharsets.UTF_8)
                val document = Jsoup.parse(html)
                val trackInfo = AudioTrackInfo {
                    uri = reference.uri
                    author = "Unknown"
                    identifier = document.selectFirst("meta[property=og:video:secure_url]")!!.attr("content")
                    title = document.selectFirst("meta[property=og:title]")!!.attr("content")
                }

                return createTrack(trackInfo)
            }
        } catch (e: IOException) {
            throw FriendlyException("Failed to load info for yarn clip", FriendlyException.Severity.SUSPICIOUS, null)
        }
    }

    companion object {
        private val GETYARN_REGEX = Pattern.compile("(?:http://|https://(?:www\\.)?)?getyarn\\.io/yarn-clip/(.*)")
    }
}