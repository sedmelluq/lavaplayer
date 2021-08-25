package lavaplayer.manager.event;

/**
 * Listener of audio events.
 */
interface AudioEventListener {
    /**
     * @param event The event
     */
    fun onEvent(event: AudioEvent)
}
