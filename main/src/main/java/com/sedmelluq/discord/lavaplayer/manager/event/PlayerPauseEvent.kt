package com.sedmelluq.discord.lavaplayer.manager.event

import com.sedmelluq.discord.lavaplayer.manager.AudioPlayer

/**
 * Event that is fired when a player is paused.
 *
 * @param player Audio player
 */
data class PlayerPauseEvent(override val player: AudioPlayer) : AudioEvent
