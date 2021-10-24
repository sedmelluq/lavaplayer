package com.sedmelluq.discord.lavaplayer.container.flac.frame

/**
 * Information of a FLAC frame that is required for reading its subframes. Most of the fields in the frame info are not
 * actually needed, since it is an error if they differ from the ones specified in the file metadata.
 *
 * @param sampleCount  Number of samples in each subframe of this frame
 * @param channelDelta Channel data delta setting
 */
data class FlacFrameInfo(
    /**
     * Number of samples in each subframe of this frame.
     */
    @JvmField val sampleCount: Int,
    /**
     * The way stereo channel data is related. With stereo frames, one channel can contain its original data and the other
     * just the difference from the first one, which allows for better compression for the other channel.
     */
    @JvmField val channelDelta: ChannelDelta
) {
    /**
     * The relationship between stereo channels.
     */
    enum class ChannelDelta(
        /**
         * The index of the channel containing delta values.
         */
        @JvmField val deltaChannel: Int
    ) {
        NONE(-1),
        LEFT_SIDE(1),
        RIGHT_SIDE(0),
        MID_SIDE(1);
    }
}
