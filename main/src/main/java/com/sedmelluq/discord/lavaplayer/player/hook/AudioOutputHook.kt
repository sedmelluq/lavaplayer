package com.sedmelluq.discord.lavaplayer.player.hook

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame

/**
 * Hook for intercepting outgoing audio frames from AudioPlayer.
 */
interface AudioOutputHook {
    /**
     * @param player Audio player where the frame is coming from
     * @param frame  Audio frame
     * @return The frame to pass onto the actual caller
     */
    fun outgoingFrame(player: AudioPlayer, frame: AudioFrame): AudioFrame
}
