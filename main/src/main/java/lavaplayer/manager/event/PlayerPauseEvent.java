package lavaplayer.manager.event;

import lavaplayer.manager.AudioPlayer;

/**
 * Event that is fired when a player is paused.
 */
public class PlayerPauseEvent extends AudioEvent {
    /**
     * @param player Audio player
     */
    public PlayerPauseEvent(AudioPlayer player) {
        super(player);
    }
}
