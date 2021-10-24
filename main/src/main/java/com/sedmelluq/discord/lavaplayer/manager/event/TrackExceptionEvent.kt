package com.sedmelluq.discord.lavaplayer.manager.event

import com.sedmelluq.discord.lavaplayer.manager.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

/**
 * Event that is fired when an exception occurs in an audio track that causes it to halt or not start.
 * @param player    Audio player
 * @param track     Audio track where the exception occurred
 * @param exception The exception that occurred
 */
data class TrackExceptionEvent(
    override val player: AudioPlayer,
    /**
     * Audio track where the exception occurred
     */
    val track: AudioTrack,
    /**
     * The exception that occurred
     */
    val exception: FriendlyException
) : AudioEvent
