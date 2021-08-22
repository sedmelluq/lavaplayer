package lavaplayer.manager.event;

import lavaplayer.manager.AudioPlayer;
import lavaplayer.tools.FriendlyException;
import lavaplayer.track.AudioTrack;

/**
 * Event that is fired when an exception occurs in an audio track that causes it to halt or not start.
 */
public class TrackExceptionEvent extends AudioEvent {
    /**
     * Audio track where the exception occurred
     */
    public final AudioTrack track;
    /**
     * The exception that occurred
     */
    public final FriendlyException exception;

    /**
     * @param player    Audio player
     * @param track     Audio track where the exception occurred
     * @param exception The exception that occurred
     */
    public TrackExceptionEvent(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        super(player);
        this.track = track;
        this.exception = exception;
    }
}
