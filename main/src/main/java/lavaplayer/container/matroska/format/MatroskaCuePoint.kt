package lavaplayer.container.matroska.format

/**
 * Matroska file cue point. Provides the offsets at a specific timecode for each track
 *
 * @param timecode            Timecode using the file timescale
 * @param trackClusterOffsets Absolute offset to the cluster
 */
data class MatroskaCuePoint(
    /**
     * Timecode using the file timescale
     */
    @JvmField val timecode: Long,
    /**
     * Absolute offset to the cluster
     */
    @JvmField val trackClusterOffsets: LongArray
)
