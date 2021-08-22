package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A helper class to consume the entire contents of a stream into a direct byte buffer. Designed for cases where this is
 * repeated several times, as it supports resetting.
 */
public class DirectBufferStreamBroker {
    private final byte[] copyBuffer;
    private final int initialSize;
    private int readByteCount;
    private ByteBuffer currentBuffer;

    /**
     * @param initialSize Initial size of the underlying direct buffer.
     */
    public DirectBufferStreamBroker(int initialSize) {
        this.initialSize = initialSize;
        this.copyBuffer = new byte[512];
        this.currentBuffer = ByteBuffer.allocateDirect(initialSize);
    }

    /**
     * Reset the buffer to its initial size.
     */
    public void resetAndCompact() {
        currentBuffer = ByteBuffer.allocateDirect(initialSize);
    }

    /**
     * Clear the underlying buffer.
     */
    public void clear() {
        currentBuffer.clear();
    }

    /**
     * @return A duplicate of the underlying buffer.
     */
    public ByteBuffer getBuffer() {
        ByteBuffer buffer = currentBuffer.duplicate();
        buffer.flip();
        return buffer;
    }

    public boolean isTruncated() {
        return currentBuffer.position() < readByteCount;
    }

    /**
     * Copies the final state after a {@link #consumeNext(InputStream, int, int)} operation into a new byte array.
     *
     * @return New byte array containing consumed data.
     */
    public byte[] extractBytes() {
        byte[] data = new byte[currentBuffer.position()];
        currentBuffer.position(0);
        currentBuffer.get(data, 0, data.length);
        return data;
    }

    /**
     * Consume an entire stream and append it into the buffer (or clear first if clear parameter is true).
     *
     * @param inputStream       The input stream to fully consume.
     * @param maximumSavedBytes Maximum number of bytes to save internally. If this is exceeded, it will continue reading
     *                          and discarding until maximum read byte count is reached.
     * @param maximumReadBytes  Maximum number of bytes to read.
     * @return If stream was fully read before {@code maximumReadBytes} was reached, returns {@code true}. Returns
     * {@code false} if the number of bytes read is {@code maximumReadBytes}, even if no more data is left in the
     * stream.
     * @throws IOException On read error
     */
    public boolean consumeNext(InputStream inputStream, int maximumSavedBytes, int maximumReadBytes) throws IOException {
        currentBuffer.clear();
        readByteCount = 0;

        ensureCapacity(Math.min(maximumSavedBytes, inputStream.available()));

        while (readByteCount < maximumReadBytes) {
            int maximumReadFragment = Math.min(copyBuffer.length, maximumReadBytes - readByteCount);
            int fragmentLength = inputStream.read(copyBuffer, 0, maximumReadFragment);

            if (fragmentLength == -1) {
                return true;
            }

            int bytesToSave = Math.min(fragmentLength, maximumSavedBytes - readByteCount);

            if (bytesToSave > 0) {
                ensureCapacity(currentBuffer.position() + bytesToSave);
                currentBuffer.put(copyBuffer, 0, bytesToSave);
            }
        }

        return false;
    }

    private void ensureCapacity(int capacity) {
        if (capacity > currentBuffer.capacity()) {
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(currentBuffer.capacity() << 1);
            currentBuffer.flip();

            newBuffer.put(currentBuffer);
            currentBuffer = newBuffer;
        }
    }
}
