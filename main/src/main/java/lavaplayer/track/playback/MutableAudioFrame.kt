package lavaplayer.track.playback

import java.nio.ByteBuffer

/**
 * A mutable audio frame.
 */
class MutableAudioFrame : AbstractMutableAudioFrame() {
    private var frameBuffer: ByteBuffer? = null
    private var framePosition = 0

    override val data: ByteArray
        get() {
            val data = ByteArray(dataLength)
            getData(data, 0)
            return data
        }

    override var dataLength = 0
        private set

    /**
     * This should be called only by the requester of a frame.
     *
     * @param frameBuffer Buffer to use internally.
     */
    fun setBuffer(frameBuffer: ByteBuffer) {
        this.frameBuffer = frameBuffer
        framePosition = frameBuffer.position()
        dataLength = frameBuffer.remaining()
    }

    /**
     * This should be called only by the provider of a frame.
     *
     * @param buffer Buffer to copy data from into the internal buffer of this instance.
     * @param offset Offset in the buffer.
     * @param length Length of the data to copy.
     */
    fun store(buffer: ByteArray, offset: Int, length: Int) {
        frameBuffer!!.position(framePosition)
        frameBuffer!!.limit(frameBuffer!!.capacity())
        frameBuffer!!.put(buffer, offset, length)
        dataLength = length
    }

    override fun getData(buffer: ByteArray, offset: Int) {
        val previous = frameBuffer!!.position()
        frameBuffer!!.position(framePosition)
        frameBuffer!![buffer, offset, dataLength]
        frameBuffer!!.position(previous)
    }
}
