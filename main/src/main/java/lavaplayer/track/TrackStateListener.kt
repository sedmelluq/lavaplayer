package lavaplayer.track

import lavaplayer.tools.FriendlyException

/**
 * Listener of track execution events.
 */
interface TrackStateListener {
    /**
     * Called when an exception occurs while a track is playing or loading. This is always fatal, but it may have left
     * some data in the audio buffer which can still play until the buffer clears out.
     *
     * @param track     The audio track for which the exception occurred
     * @param exception The exception that occurred
     */
    fun onTrackException(track: AudioTrack, exception: FriendlyException)

    /**
     * Called when an exception occurs while a track is playing or loading. This is always fatal, but it may have left
     * some data in the audio buffer which can still play until the buffer clears out.
     *
     * @param track       The audio track for which the exception occurred
     * @param thresholdMs The wait threshold that was exceeded for this event to trigger
     */
    fun onTrackStuck(track: AudioTrack, thresholdMs: Long)
}
