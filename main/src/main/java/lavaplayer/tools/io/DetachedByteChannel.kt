package lavaplayer.tools.io

import java.nio.channels.ReadableByteChannel
import kotlin.Throws
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException

/**
 * Creates a readable byte channel which can be closed without closing the underlying channel.
 *
 * @param delegate The underlying channel
 */
class DetachedByteChannel(private val delegate: ReadableByteChannel) : ReadableByteChannel {
    private var closed = false

    @Throws(IOException::class)
    override fun read(output: ByteBuffer): Int {
        if (closed) {
            throw ClosedChannelException()
        }

        return delegate.read(output)
    }

    override fun isOpen(): Boolean {
        return !closed && delegate.isOpen
    }

    @Throws(IOException::class)
    override fun close() {
        closed = true
    }
}
