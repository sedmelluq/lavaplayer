package com.sedmelluq.discord.lavaplayer.track

/**
 * A track position marker. This makes the specified handler get called when the specified position is reached or
 * reaching that position has become impossible. This guarantees that whenever a marker is set and the track is played,
 * its handler will always be called.
 *
 * @param timecode The position of the track in milliseconds when this marker should trigger.
 * @param handler  The handler for the marker.
 */
class TrackMarker(
    /**
     * The position of the track in milliseconds when this marker should trigger.
     */
    @JvmField
    val timecode: Long,

    /**
     * The handler for the marker. The handler is guaranteed to be never called more than once, and guaranteed to be
     * called at least once if the track is started on a player.
     */
    @JvmField
    val handler: TrackMarkerHandler
)
