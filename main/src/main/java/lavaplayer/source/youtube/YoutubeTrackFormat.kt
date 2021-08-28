package lavaplayer.source.youtube

import org.apache.http.entity.ContentType

import java.net.URI
import java.net.URISyntaxException

/**
 * Describes an available media format for a track
 *
 * @param type          Mime type of the format
 * @param bitrate       Bitrate of the format
 * @param contentLength Length in bytes of the media
 * @param baseUrl           Base URL for the playback of this format
 * @param signature     Cipher signature for this format
 * @param signatureKey  The key to use for deciphered signature in the final playback URL
 */
data class YoutubeTrackFormat(
    /**
     * Mime type of the format
     */
    val type: ContentType,
    /**
     * @return Bitrate of the format
     */
    val bitrate: Long,
    /**
     * Length in bytes of the media
     */
    val contentLength: Long,
    private val baseUrl: String,
    /**
     * Cipher signature for this format
     */
    val signature: String?,
    /**
     * The key to use for deciphered signature in the final playback URL
     */
    val signatureKey: String
) {
    /**
     * Format container and codec info
     */
    val info = YoutubeFormatInfo.get(type)

    /**
     * Base URL for the playback of this format
     */
    val url: URI
        get() = try {
            URI(baseUrl)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
}
