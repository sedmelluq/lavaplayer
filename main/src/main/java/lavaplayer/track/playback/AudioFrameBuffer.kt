package lavaplayer.track.playback

import java.lang.InterruptedException

/**
 * A frame buffer. Stores the specified duration worth of frames in the internal buffer.
 * Frames are consumed in a blocking manner and provides frames in a non-blocking manner.
 */
interface AudioFrameBuffer : AudioFrameProvider, AudioFrameConsumer {
    /**
     * Number of frames that can be added to the buffer without blocking.
     */
    val remainingCapacity: Int

    /**
     * Total number of frames that the buffer can hold.
     */
    val fullCapacity: Int

    /**
     * The timecode of the last frame in the buffer, null if the buffer is empty or is marked to be cleared upon
     * receiving the next frame.
     */
    val lastInputTimecode: Long?

    /**
     * Wait until another thread has consumed a terminator frame from this buffer
     *
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun waitForTermination()

    /**
     * Signal that no more input is expected and if the content frames have been consumed, emit a terminator frame.
     */
    fun setTerminateOnEmpty()

    /**
     * Signal that the next frame provided to the buffer will clear the frames before it. This is useful when the next
     * data is not contiguous with the current frame buffer, but the remaining frames in the buffer should be used until
     * the next data arrives to prevent a situation where the buffer cannot provide any frames for a while.
     */
    fun setClearOnInsert()

    /**
     * @return Whether the next frame is set to clear the buffer.
     */
    fun hasClearOnInsert(): Boolean

    /**
     * Clear the buffer.
     */
    fun clear()

    /**
     * Lock the buffer so no more incoming frames are accepted.
     */
    fun lockBuffer()

    /**
     * @return True if this buffer has received any input frames.
     */
    fun hasReceivedFrames(): Boolean
}
