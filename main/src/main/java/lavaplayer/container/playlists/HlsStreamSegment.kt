package lavaplayer.container.playlists

data class HlsStreamSegment(
    /**
     * URL of the segment.
     */
    @JvmField val url: String,
    /**
     * Duration of the segment in milliseconds. `null` if unknown.
     */
    @JvmField val duration: Long?,
    /**
     * Name of the segment. `null` if unknown.
     */
    @JvmField val name: String?
)
