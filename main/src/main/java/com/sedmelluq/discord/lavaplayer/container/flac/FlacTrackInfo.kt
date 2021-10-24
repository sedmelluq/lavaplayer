package com.sedmelluq.discord.lavaplayer.container.flac

/**
 * All relevant information about a FLAC track from its metadata.
 */
class FlacTrackInfo(
    /**
     * FLAC stream information.
     */
    @JvmField val stream: FlacStreamInfo,
    /**
     * An array of seek points.
     */
    @JvmField val seekPoints: Array<FlacSeekPoint>,
    /**
     * The actual number of seek points that are not placeholders. The end of the array may contain empty seek points,
     * which is why this value should be used to determine how far into the array to look.
     */
    @JvmField val seekPointCount: Int,
    /**
     * The map of tag values from comment metadata block.
     */
    @JvmField val tags: Map<String, String>,
    /**
     * The position in the stream where the first frame starts.
     */
    @JvmField val firstFramePosition: Long
) {
    /**
     * The duration of the track in milliseconds
     */
    @JvmField
    val duration: Long = stream.sampleCount * 1000L / stream.sampleRate
}
