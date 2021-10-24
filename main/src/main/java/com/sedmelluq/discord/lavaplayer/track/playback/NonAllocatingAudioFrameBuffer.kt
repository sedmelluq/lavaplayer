package com.sedmelluq.discord.lavaplayer.track.playback

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.tools.extensions.notifyAll
import com.sedmelluq.discord.lavaplayer.tools.extensions.wait
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Audio frame buffer implementation which never allocates any new objects after creation. All instances of mutable
 * frames are pre-allocated, and for the data there is one byte buffer which is used as a ring buffer for the frame data.
 *
 * @param bufferDuration The length of the internal buffer in milliseconds
 * @param format         The format of the frames held in this buffer
 * @param stopping       Atomic boolean which has true value when the track is in a state of pending stop.
 */
class NonAllocatingAudioFrameBuffer(bufferDuration: Int, format: AudioDataFormat, private val stopping: (() -> Boolean)?) :
    AbstractAudioFrameBuffer(format) {
    /**
     * @return Total number of frames that the buffer can hold.
     */
    override val fullCapacity: Int
    private val frames: Array<ReferenceMutableAudioFrame?>
    private val silentFrame: ReferenceMutableAudioFrame
    private val frameBuffer: ByteArray
    private var bridgeFrame: MutableAudioFrame? = null
    private var firstFrame = 0
    private var frameCount = 0

    /**
     * @return Number of frames that can be added to the buffer without blocking.
     */
    override val remainingCapacity: Int
        get() {
            synchronized(synchronizer) {
                if (frameCount == 0) {
                    return fullCapacity
                }
                val lastFrame = wrappedFrameIndex(firstFrame + frameCount - 1)
                val bufferHead = frames[firstFrame]!!.frameOffset
                val bufferTail = frames[lastFrame]!!.frameEndOffset
                val maximumFrameSize = format.maximumChunkSize
                return if (bufferHead < bufferTail) {
                    (frameBuffer.size - bufferTail) / maximumFrameSize + bufferHead / maximumFrameSize
                } else {
                    (bufferHead - bufferTail) / maximumFrameSize
                }
            }
        }

    init {
        val maximumFrameCount = bufferDuration / format.frameDuration().toInt() + 1
        frames = createFrames(maximumFrameCount, format)
        silentFrame = createSilentFrame(format)
        frameBuffer = ByteArray(format.expectedChunkSize * maximumFrameCount)
        fullCapacity = frameBuffer.size / format.maximumChunkSize
    }

    @Throws(InterruptedException::class)
    override fun consume(frame: AudioFrame) {
        // If an interrupt sent along with setting the stopping status was silently consumed elsewhere, this check should
        // still trigger. Guarantees that stopped tracks cannot get stuck in this method. Possible performance improvement:
        // offer with timeout, check stopping if timed out, then put?
        if (stopping?.invoke() == true) {
            throw InterruptedException()
        }

        synchronized(synchronizer) {
            if (!locked) {
                receivedFrames = true
                if (clearOnInsert) {
                    clear()
                    clearOnInsert = false
                }
                while (!attemptStore(frame)) {
                    synchronizer.wait()
                }
                synchronizer.notifyAll()
            }
        }
    }

    override fun provide(): AudioFrame? {
        synchronized(synchronizer) {
            return if (provide(getBridgeFrame())) {
                unwrapBridgeFrame()
            } else null
        }
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(timeout: Long, unit: TimeUnit): AudioFrame? {
        synchronized(synchronizer) {
            return if (provide(getBridgeFrame(), timeout, unit)) {
                unwrapBridgeFrame()
            } else null
        }
    }

    override fun provide(targetFrame: MutableAudioFrame): Boolean {
        synchronized(synchronizer) {
            return if (frameCount == 0) {
                if (terminateOnEmpty) {
                    popPendingTerminator(targetFrame)
                    synchronizer.notifyAll()
                    return true
                }
                false
            } else {
                popFrame(targetFrame)
                synchronizer.notifyAll()
                true
            }
        }
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(targetFrame: MutableAudioFrame, timeout: Long, unit: TimeUnit): Boolean {
        var currentTime = System.nanoTime()
        val endTime = currentTime + unit.toMillis(timeout)
        synchronized(synchronizer) {
            while (frameCount == 0) {
                if (terminateOnEmpty) {
                    popPendingTerminator(targetFrame)
                    synchronizer.notifyAll()
                    return true
                }
                synchronizer.wait(endTime - currentTime)
                currentTime = System.nanoTime()
                if (currentTime >= endTime) {
                    throw TimeoutException()
                }
            }
            popFrame(targetFrame)
            synchronizer.notifyAll()
            return true
        }
    }

    private fun popFrame(targetFrame: MutableAudioFrame) {
        var frame = frames[firstFrame]
        if (frame!!.volume == 0) {
            silentFrame.timecode = frame.timecode
            frame = silentFrame
        }
        targetFrame.timecode = frame.timecode
        targetFrame.volume = frame.volume
        targetFrame.isTerminator = false
        targetFrame.store(frame.frameBuffer, frame.frameOffset, frame.dataLength)
        firstFrame = wrappedFrameIndex(firstFrame + 1)
        frameCount--
    }

    private fun popPendingTerminator(frame: MutableAudioFrame) {
        terminateOnEmpty = false
        terminated = true
        frame.isTerminator = true
    }

    override fun clear() {
        synchronized(synchronizer) { frameCount = 0 }
    }

    override fun rebuild(rebuilder: AudioFrameRebuilder) {
        log.debug("Frame rebuild not supported on non-allocating frame buffer yet.")
    }

    override val lastInputTimecode: Long?
        get() {
            synchronized(synchronizer) {
                if (!clearOnInsert && frameCount > 0) {
                    return frames[wrappedFrameIndex(firstFrame + frameCount - 1)]!!.timecode
                }
            }
            return null
        }

    private fun attemptStore(frame: AudioFrame): Boolean {
        if (frameCount >= frames.size) {
            return false
        }
        val frameLength = frame.dataLength
        val frameBufferLength = frameBuffer.size
        if (frameCount == 0) {
            firstFrame = 0
            require(frameLength <= frameBufferLength) { "Frame is too big for buffer." }
            store(frame, 0, 0, frameLength)
        } else {
            val lastFrame = wrappedFrameIndex(firstFrame + frameCount - 1)
            val nextFrame = wrappedFrameIndex(lastFrame + 1)
            val bufferHead = frames[firstFrame]!!.frameOffset
            val bufferTail = frames[lastFrame]!!.frameEndOffset
            if (bufferHead < bufferTail) {
                if (bufferTail + frameLength <= frameBufferLength) {
                    store(frame, nextFrame, bufferTail, frameLength)
                } else if (bufferHead >= frameLength) {
                    store(frame, nextFrame, 0, frameLength)
                } else {
                    return false
                }
            } else if (bufferTail + frameLength <= bufferHead) {
                store(frame, nextFrame, bufferTail, frameLength)
            } else {
                return false
            }
        }
        return true
    }

    private fun wrappedFrameIndex(index: Int): Int {
        val maximumFrameCount = frames.size
        return if (index >= maximumFrameCount) index - maximumFrameCount else index
    }

    private fun store(frame: AudioFrame, index: Int, frameOffset: Int, frameLength: Int) {
        val targetFrame = frames[index]
        targetFrame!!.timecode = frame.timecode
        targetFrame.volume = frame.volume
        targetFrame.setDataReference(frameBuffer, frameOffset, frameLength)
        frame.getData(frameBuffer, frameOffset)
        frameCount++
    }

    private fun getBridgeFrame(): MutableAudioFrame {
        if (bridgeFrame == null) {
            bridgeFrame = MutableAudioFrame()
            bridgeFrame!!.setBuffer(ByteBuffer.allocate(format.maximumChunkSize))
        }
        return bridgeFrame!!
    }

    private fun unwrapBridgeFrame(): AudioFrame {
        return if (bridgeFrame!!.isTerminator) {
            TerminatorAudioFrame
        } else {
            ImmutableAudioFrame(
                bridgeFrame!!.timecode,
                bridgeFrame!!.data,
                bridgeFrame!!.volume,
                bridgeFrame!!.format!!
            )
        }
    }

    override fun signalWaiters() {
        synchronized(synchronizer) { synchronizer.notifyAll() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NonAllocatingAudioFrameBuffer::class.java)
        private fun createFrames(frameCount: Int, format: AudioDataFormat): Array<ReferenceMutableAudioFrame?> {
            val frames = arrayOfNulls<ReferenceMutableAudioFrame>(frameCount)
            for (i in frames.indices) {
                frames[i] = ReferenceMutableAudioFrame()
                frames[i]!!.format = format
            }
            return frames
        }

        private fun createSilentFrame(format: AudioDataFormat): ReferenceMutableAudioFrame {
            val frame = ReferenceMutableAudioFrame()
            frame.format = format
            frame.setDataReference(format.silenceBytes(), 0, format.silenceBytes().size)
            frame.volume = 0
            return frame
        }
    }
}
