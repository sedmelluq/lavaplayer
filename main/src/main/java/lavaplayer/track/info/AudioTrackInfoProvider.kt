package lavaplayer.track.info

/**
 * Provider for audio track info.
 */
interface AudioTrackInfoProvider {
    /**
     * @return Track title, or `null` if this provider does not know it.
     */
    val title: String?

    /**
     * @return Track author, or `null` if this provider does not know it.
     */
    val author: String?

    /**
     * @return Track length in milliseconds, or `null` if this provider does not know it.
     */
    val length: Long?

    /**
     * @return Track identifier, or `null` if this provider does not know it.
     */
    val identifier: String?

    /**
     * @return Track URI, or `null` if this provider does not know it.
     */
    val uri: String?

    /**
     * @return Track Artwork URL, or `null` if this provider does not know it.
     */
    val artworkUrl: String?
}
