package com.sedmelluq.discord.lavaplayer.manager.event

import com.sedmelluq.discord.lavaplayer.manager.AudioPlayer

/**
 * An event related to an audio player.
 */
interface AudioEvent {
    /**
     * The related audio player.
     */
    val player: AudioPlayer
}
