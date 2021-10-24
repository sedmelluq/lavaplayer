package com.sedmelluq.discord.lavaplayer.track.playback

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Encapsulates common behavior shared by different audio frame providers.
 */
object AudioFrameProviderTools {
    /**
     * @param provider Delegates a call to frame provide without timeout to the timed version of it.
     * @return The audio frame from provide method.
     */
    @JvmStatic
    fun delegateToTimedProvide(provider: AudioFrameProvider): AudioFrame? {
        try {
            return provider.provide(0, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            ExceptionTools.keepInterrupted(e)
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            ExceptionTools.keepInterrupted(e)
            throw RuntimeException(e)
        }
    }
}
