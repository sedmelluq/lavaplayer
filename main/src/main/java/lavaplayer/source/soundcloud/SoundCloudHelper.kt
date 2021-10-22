package lavaplayer.source.soundcloud

import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.io.PersistentHttpStream
import lavaplayer.tools.json.JsonBrowser
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.net.URI

object SoundCloudHelper {
    @JvmStatic
    fun nonMobileUrl(url: String): String {
        return url.takeIf { !url.startsWith("https://m.") }
            ?: "https://${url.substring("https://m.".length)}"
    }

    @Throws(IOException::class)
    @JvmStatic
    fun loadPlaybackUrl(httpInterface: HttpInterface, jsonUrl: String): String {
        PersistentHttpStream(httpInterface, URI.create(jsonUrl), null).use { stream ->
            if (!HttpClientTools.isSuccessWithContent(stream.checkStatusCode())) {
                throw IOException("Invalid status code for soundcloud stream: " + stream.checkStatusCode())
            }

            return JsonBrowser.parse(stream)["url"].safeText
        }
    }
}
