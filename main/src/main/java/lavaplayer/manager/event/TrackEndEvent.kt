package lavaplayer.manager.event;

import lavaplayer.manager.AudioPlayer;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackEndReason;

/**
 * Event that is fired when an audio track ends in an audio player, either by interruption, exception or reaching the end.
 *
 * @param player    Audio player
 * @param track     Audio track that ended
 * @param endReason The reason why the track stopped playing
 */
data class TrackEndEvent(
    override val player: AudioPlayer,
    /**
     * Audio track that ended
     */
    val track: AudioTrack,
    /**
     * The reason why the track stopped playing
     */
    val endReason: AudioTrackEndReason,
) : AudioEvent
