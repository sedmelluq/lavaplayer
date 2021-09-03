package lavaplayer.source.youtube

import lavaplayer.tools.io.HttpInterface
import java.io.IOException
import java.net.URI


interface YoutubeSignatureResolver {
    @Throws(Exception::class)
    fun resolveFormatUrl(httpInterface: HttpInterface, playerScript: String?, format: YoutubeTrackFormat): URI

    @Throws(Exception::class)
    fun resolveDashUrl(httpInterface: HttpInterface, playerScript: String?, dashUrl: String): String

    @Throws(IOException::class)
    fun getCipherKeyAndTimestampFromScript(httpInterface: HttpInterface, playerScript: String): YoutubeSignatureCipher?
}
