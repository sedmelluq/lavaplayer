package com.sedmelluq.discord.lavaplayer.manager.event

/**
 * Listener of audio events.
 */
fun interface AudioEventListener {
    /**
     * @param event The event
     */
    suspend fun onEvent(event: AudioEvent)
}
