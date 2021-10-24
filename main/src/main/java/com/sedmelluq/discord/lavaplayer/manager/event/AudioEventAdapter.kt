package com.sedmelluq.discord.lavaplayer.manager.event

import com.sedmelluq.discord.lavaplayer.manager.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason

/**
 * Adapter for different event handlers as method overrides
 */
abstract class AudioEventAdapter : AudioEventListener {
    /**
     * @param player Audio player
     */
    open fun onPlayerPause(player: AudioPlayer) {}

    /**
     * @param player Audio player
     */
    open fun onPlayerResume(player: AudioPlayer) {}

    /**
     * @param player Audio player
     * @param track  Audio track that started
     */
    open fun onTrackStart(player: AudioPlayer, track: AudioTrack) {}

    /**
     * @param player    Audio player
     * @param track     Audio track that ended
     * @param endReason The reason why the track stopped playing
     */
    open fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {}

    /**
     * @param player    Audio player
     * @param track     Audio track where the exception occurred
     * @param exception The exception that occurred
     */
    open fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {}

    /**
     * @param player      Audio player
     * @param track       Audio track where the exception occurred
     * @param thresholdMs The wait threshold that was exceeded for this event to trigger
     */
    open fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {}

    open fun onTrackStuck(
        player: AudioPlayer,
        track: AudioTrack,
        thresholdMs: Long,
        stackTrace: List<StackTraceElement>
    ) {
        onTrackStuck(player, track, thresholdMs)
    }

    override suspend fun onEvent(event: AudioEvent) {
        when (event) {
            is PlayerPauseEvent -> onPlayerPause(event.player)
            is PlayerResumeEvent -> onPlayerResume(event.player)
            is TrackStartEvent -> onTrackStart(event.player, event.track)
            is TrackEndEvent -> onTrackEnd(event.player, event.track, event.endReason)
            is TrackExceptionEvent -> onTrackException(event.player, event.track, event.exception)
            is TrackStuckEvent -> onTrackStuck(event.player, event.track, event.thresholdMs, event.stackTrace)
        }
    }
}
