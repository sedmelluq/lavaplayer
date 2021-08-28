package lavaplayer.source.soundcloud

import lavaplayer.tools.DataFormatTools
import lavaplayer.tools.DataFormatTools.TextRange
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS
import lavaplayer.tools.JsonBrowser
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpInterface
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets

class DefaultSoundCloudHtmlDataLoader : SoundCloudHtmlDataLoader {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(DefaultSoundCloudHtmlDataLoader::class.java)
        private val JSON_RANGES: List<TextRange> = listOf(
            TextRange("window.__sc_hydration =", ";</script>"),
            TextRange("catch(e){}})},", ");</script>"),
            TextRange("){}})},", ");</script>"),
        )
    }


    @Throws(IOException::class)
    override fun load(httpInterface: HttpInterface, url: String): JsonBrowser {
        httpInterface.execute(HttpGet(url)).use { response ->
            if (response.statusLine.statusCode == HttpStatus.SC_NOT_FOUND) {
                return JsonBrowser.NULL_BROWSER
            }

            HttpClientTools.assertSuccessWithContent(response, "video page response")

            val html = EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
            val rootData = DataFormatTools.extractBetween(html, JSON_RANGES)
                ?: throw FriendlyException(
                    "This url does not appear to be a playable track.", SUSPICIOUS,
                    ExceptionTools.throwWithDebugInfo(log, null, "No track JSON found", "html", html)
                )

            return JsonBrowser.parse(rootData)
        }
    }
}
