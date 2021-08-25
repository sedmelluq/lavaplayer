package lavaplayer.source.youtube

import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.io.PersistentHttpStream
import org.apache.http.client.utils.URIBuilder

import java.net.URI
import java.net.URISyntaxException

/**
 * A persistent HTTP stream implementation that uses the range parameter instead of HTTP headers for specifying
 * the start position at which to start reading on a new connection.
 *
 * @param httpInterface The HTTP interface to use for requests
 * @param contentUrl    The URL of the resource
 * @param contentLength The length of the resource in bytes
 */
class YoutubePersistentHttpStream(
    httpInterface: HttpInterface,
    contentUrl: URI,
    contentLength: Long
) : PersistentHttpStream(httpInterface, contentUrl, contentLength) {

    override fun getConnectUrl(): URI? {
        if (position < 0) {
            return contentUrl
        }

        try {
            return URIBuilder(contentUrl)
                .addParameter("range", "$position-$contentLength")
                .build()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    override fun useHeadersForRange() =
        false

    override fun canSeekHard() =
        true
}
