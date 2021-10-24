package com.sedmelluq.discord.lavaplayer.track.playback

/**
 * Interface for classes which can rebuild audio frames.
 */
interface AudioFrameRebuilder {
    /**
     * Rebuilds a frame (for example by re-encoding)
     *
     * @param frame The audio frame
     * @return The new frame (might be the same as input)
     */
    fun rebuild(frame: AudioFrame): AudioFrame
}
