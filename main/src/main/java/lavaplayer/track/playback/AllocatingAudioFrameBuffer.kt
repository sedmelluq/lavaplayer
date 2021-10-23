package lavaplayer.track.playback

import kotlinx.atomicfu.AtomicBoolean
import lavaplayer.format.AudioDataFormat
import lavaplayer.tools.extensions.notifyAll
import mu.KotlinLogging
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A frame buffer. Stores the specified duration worth of frames in the internal buffer.
 * Consumes [AudioFrames][AudioFrame] in a blocking manner and provides frames in a non-blocking manner.
 *
 * @param bufferDuration The length of the internal buffer in milliseconds
 * @param format         The format of the frames held in this buffer
 * @param stopping       Atomic boolean which has true value when the track is in a state of pending stop.
 */
class AllocatingAudioFrameBuffer(bufferDuration: Int, format: AudioDataFormat, private val stopping: AtomicBoolean?) : AbstractAudioFrameBuffer(format) {
    companion object {
        private val log = KotlinLogging.logger {  }
    }

    override val fullCapacity: Int = bufferDuration / 20 + 1
    override val remainingCapacity get() = audioFrames.remainingCapacity()
    override val lastInputTimecode: Long
        get() {
            var lastTimecode: Long? = null
            synchronized(synchronizer) {
                if (!clearOnInsert) {
                    for (frame in audioFrames) {
                        lastTimecode = frame.timecode
                    }
                }
            }

            return lastTimecode!!
        }


    private val audioFrames: ArrayBlockingQueue<AudioFrame> = ArrayBlockingQueue(fullCapacity)

    override fun provide(): AudioFrame? {
        val frame = audioFrames.poll()
            ?: return fetchPendingTerminator()

        if (frame.isTerminator) {
            fetchPendingTerminator()
            return frame
        }

        return filterFrame(frame)
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(timeout: Long, unit: TimeUnit): AudioFrame? {
        var frame = audioFrames.poll()
        if (frame == null) {
            var terminator = fetchPendingTerminator()
            if (terminator != null) {
                return terminator
            }

            if (timeout > 0) {
                frame = audioFrames.poll(timeout, unit)
                if (frame == null || frame.isTerminator) {
                    terminator = fetchPendingTerminator()
                    return terminator ?: frame
                }
            }
        } else if (frame.isTerminator) {
            fetchPendingTerminator()
            return frame
        }

        return filterFrame(frame)
    }

    override fun provide(targetFrame: MutableAudioFrame): Boolean {
        return passToMutable(provide(), targetFrame)
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(targetFrame: MutableAudioFrame, timeout: Long, unit: TimeUnit): Boolean {
        return passToMutable(provide(timeout, unit), targetFrame)
    }

    private fun passToMutable(frame: AudioFrame?, targetFrame: MutableAudioFrame?): Boolean {
        if (targetFrame != null && frame != null) {
            if (frame.isTerminator) {
                targetFrame.isTerminator = true
            } else {
                targetFrame.timecode = frame.timecode
                targetFrame.volume = frame.volume
                targetFrame.store(frame.data, 0, frame.dataLength)
                targetFrame.isTerminator = false
            }

            return true
        }

        return false
    }

    override fun clear() {
        audioFrames.clear()
    }

    override fun rebuild(rebuilder: AudioFrameRebuilder) {
        val frames = mutableListOf<AudioFrame>()
        val frameCount = audioFrames.drainTo(frames)

        log.debug { "Running re-builder ${rebuilder.javaClass.simpleName} on $frameCount buffered frames." }
        for (frame in frames) {
            audioFrames.add(rebuilder.rebuild(frame))
        }
    }

    @Throws(InterruptedException::class)
    override fun consume(frame: AudioFrame) {
        // If an interrupt sent along with setting the stopping status was silently consumed elsewhere, this check should
        // still trigger. Guarantees that stopped tracks cannot get stuck in this method. Possible performance improvement:
        // offer with timeout, check stopping if timed out, then put?
        var frame = frame
        if (stopping != null && stopping.value) {
            throw InterruptedException()
        }

        if (!locked) {
            receivedFrames = true
            if (clearOnInsert) {
                audioFrames.clear()
                clearOnInsert = false
            }

            if (frame is AbstractMutableAudioFrame) {
                frame = frame.freeze()
            }

            audioFrames.put(frame)
        }
    }

    private fun fetchPendingTerminator(): AudioFrame? {
        synchronized(synchronizer) {
            if (terminateOnEmpty) {
                terminateOnEmpty = false
                terminated = true
                synchronizer.notifyAll()
                return TerminatorAudioFrame
            }
        }

        return null
    }

    private fun filterFrame(frame: AudioFrame?): AudioFrame? {
        return if (frame == null || frame.volume != 0) frame else ImmutableAudioFrame(frame.timecode, format.silenceBytes(), 0, format)
    }

    override fun signalWaiters() {
        audioFrames.offer(TerminatorAudioFrame)
    }
}
