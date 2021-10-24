package com.sedmelluq.discord.lavaplayer.manager.event

import com.sedmelluq.discord.lavaplayer.manager.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

/**
 * Event that is fired when a track starts playing.
 *
 * @param player Audio player
 * @param track  Audio track that started
 */
data class TrackStartEvent(
    override val player: AudioPlayer,
    /**
     * Audio track that started
     */
    val track: AudioTrack
) : AudioEvent
