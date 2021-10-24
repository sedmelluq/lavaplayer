package com.sedmelluq.discord.lavaplayer.track.playback

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.tools.extensions.wait

/**
 * Common parts of a frame buffer which are not likely to depend on the specific implementation.
 */
abstract class AbstractAudioFrameBuffer protected constructor(protected val format: AudioDataFormat) : AudioFrameBuffer {
    @JvmField
    protected val synchronizer: Any = Any()

    @JvmField
    @Volatile
    protected var locked: Boolean = false

    @JvmField
    @Volatile
    protected var receivedFrames: Boolean = false

    @JvmField
    protected var terminated: Boolean = false

    @JvmField
    protected var terminateOnEmpty: Boolean = false

    @JvmField
    protected var clearOnInsert: Boolean = false

    @Throws(InterruptedException::class)
    override fun waitForTermination() {
        synchronized(synchronizer) {
            while (!terminated) {
                synchronizer.wait()
            }
        }
    }

    override fun setTerminateOnEmpty() {
        synchronized(synchronizer) {
            if (clearOnInsert) {
                clear()
                clearOnInsert = false
            }

            if (!terminated) {
                terminateOnEmpty = true
                signalWaiters()
            }
        }
    }

    override fun setClearOnInsert() {
        synchronized(synchronizer) {
            clearOnInsert = true
            terminateOnEmpty = false
        }
    }

    override fun hasClearOnInsert(): Boolean {
        return clearOnInsert
    }

    override fun lockBuffer() {
        locked = true
    }

    override fun hasReceivedFrames(): Boolean {
        return receivedFrames
    }

    protected abstract fun signalWaiters()
}
