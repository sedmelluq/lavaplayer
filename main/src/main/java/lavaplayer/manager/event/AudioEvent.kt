package lavaplayer.manager.event;

import lavaplayer.manager.AudioPlayer;

/**
 * An event related to an audio player.
 */
interface AudioEvent {
    /**
     * The related audio player.
     */
    val player: AudioPlayer
}
