package lavaplayer.manager.hook

/**
 * Factory for audio output hook instances.
 */
interface AudioOutputHookFactory {
    /**
     * @return New instance of an audio output hook
     */
    fun createOutputHook(): AudioOutputHook
}
