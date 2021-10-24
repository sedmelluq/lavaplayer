package com.sedmelluq.discord.lavaplayer.manager.event

import com.sedmelluq.discord.lavaplayer.manager.AudioPlayer

/**
 * Event that is fired when a player is resumed.
 * @param player Audio player
 */
data class PlayerResumeEvent(override val player: AudioPlayer) : AudioEvent
