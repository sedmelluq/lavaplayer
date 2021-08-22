package lavaplayer.manager.event;

/**
 * Listener of audio events.
 */
public interface AudioEventListener {
    /**
     * @param event The event
     */
    void onEvent(AudioEvent event);
}
