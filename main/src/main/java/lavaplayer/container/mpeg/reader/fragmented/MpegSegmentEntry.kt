package lavaplayer.container.mpeg.reader.fragmented

/**
 * Information about one MP4 segment aka fragment
 *
 * @param type     Type of the segment
 * @param size     Size in bytes
 * @param duration Duration using the timescale of the file
 */
data class MpegSegmentEntry(
    /**
     * Type of the segment
     */
    @JvmField val type: Int,
    /**
     * Size in bytes
     */
    @JvmField val size: Int,
    /**
     * Duration using the timescale of the file
     */
    @JvmField val duration: Int
)
