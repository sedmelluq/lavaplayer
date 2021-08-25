package lavaplayer.track

import java.util.concurrent.atomic.AtomicReference
import lavaplayer.track.TrackMarkerHandler.MarkerState

/**
 * Tracks the state of a track position marker.
 */
class TrackMarkerManager {
    private val current = AtomicReference<TrackMarker?>()

    /**
     * Set a new track position marker.
     *
     * @param marker          Marker
     * @param currentTimecode Current timecode of the track when this marker is set
     */
    operator fun set(marker: TrackMarker?, currentTimecode: Long) {
        val previous = current.getAndSet(marker)
        previous?.handler?.handle(if (marker != null) MarkerState.OVERWRITTEN else MarkerState.REMOVED)
        if (marker != null && currentTimecode >= marker.timecode) {
            trigger(marker, MarkerState.LATE)
        }
    }

    /**
     * Remove the current marker.
     *
     * @return The removed marker.
     */
    fun remove(): TrackMarker? {
        return current.getAndSet(null)
    }

    /**
     * Trigger and remove the marker with the specified state.
     *
     * @param state The state of the marker to pass to the handler.
     */
    fun trigger(state: MarkerState?) {
        val marker = current.getAndSet(null)
        marker?.handler?.handle(state!!)
    }

    /**
     * Check a timecode which was reached by normal playback, trigger REACHED if necessary.
     *
     * @param timecode Timecode which was reached by normal playback.
     */
    fun checkPlaybackTimecode(timecode: Long) {
        val marker = current.get()
        if (marker != null && timecode >= marker.timecode) {
            trigger(marker, MarkerState.REACHED)
        }
    }

    /**
     * Check a timecode which was reached by seeking, trigger BYPASSED if necessary.
     *
     * @param timecode Timecode which was reached by seeking.
     */
    fun checkSeekTimecode(timecode: Long) {
        val marker = current.get()
        if (marker != null && timecode >= marker.timecode) {
            trigger(marker, MarkerState.BYPASSED)
        }
    }

    private fun trigger(marker: TrackMarker, state: MarkerState) {
        if (current.compareAndSet(marker, null)) {
            marker.handler.handle(state)
        }
    }
}
