package lavaplayer.source.youtube

import lavaplayer.tools.io.HttpInterface
import java.net.URI

interface YoutubeSignatureResolver {
    @Throws(Exception::class)
    fun resolveFormatUrl(httpInterface: HttpInterface, playerScript: String?, format: YoutubeTrackFormat): URI

    @Throws(Exception::class)
    fun resolveDashUrl(httpInterface: HttpInterface, playerScript: String?, dashUrl: String): String
}
