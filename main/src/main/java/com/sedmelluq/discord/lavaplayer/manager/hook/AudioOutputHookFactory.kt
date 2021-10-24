package com.sedmelluq.discord.lavaplayer.manager.hook

/**
 * Factory for audio output hook instances.
 */
fun interface AudioOutputHookFactory {
    /**
     * @return New instance of an audio output hook
     */
    fun createOutputHook(): AudioOutputHook
}
