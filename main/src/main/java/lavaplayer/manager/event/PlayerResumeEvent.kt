package lavaplayer.manager.event;

import lavaplayer.manager.AudioPlayer;

/**
 * Event that is fired when a player is resumed.
 * @param player Audio player
 */
data class PlayerResumeEvent(override val player: AudioPlayer) : AudioEvent
