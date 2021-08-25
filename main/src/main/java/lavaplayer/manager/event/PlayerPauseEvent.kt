package lavaplayer.manager.event;

import lavaplayer.manager.AudioPlayer;

/**
 * Event that is fired when a player is paused.
 *
 * @param player Audio player
 */
data class PlayerPauseEvent(override val player: AudioPlayer) : AudioEvent
