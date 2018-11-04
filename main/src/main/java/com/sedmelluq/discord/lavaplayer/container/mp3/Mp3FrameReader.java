package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder.HEADER_SIZE;

/**
 * Handles reading MP3 frames from a stream.
 */
public class Mp3FrameReader {
  private final SeekableInputStream inputStream;
  private final DataInput dataInput;
  private final byte[] scanBuffer;
  private final byte[] frameBuffer;
  private int frameSize;
  private int frameBufferPosition;
  private int scanBufferPosition;
  private boolean frameHeaderRead;

  /**
   * @param inputStream Input buffer to read from
   * @param frameBuffer Array to store the frame data in
   */
  public Mp3FrameReader(SeekableInputStream inputStream, byte[] frameBuffer) {
    this.inputStream = inputStream;
    this.dataInput = new DataInputStream(inputStream);
    this.scanBuffer = new byte[16];
    this.frameBuffer = frameBuffer;
  }

  /**
   * @param bytesToCheck The maximum number of bytes to check before throwing an IllegalStateException
   * @param throwOnLimit Whether to throw an exception when maximum number of bytes is reached, but no frame has been
   *                     found and EOF has not been reached.
   * @return True if a frame was found, false if EOF was encountered.
   * @throws IOException On IO error
   * @throws IllegalStateException If the maximum number of bytes to check was reached before a frame was found
   */
  public boolean scanForFrame(int bytesToCheck, boolean throwOnLimit) throws IOException {
    int bytesInBuffer = scanBufferPosition;
    scanBufferPosition = 0;

    if (parseFrameAt(bytesInBuffer)) {
      frameHeaderRead = true;
      return true;
    }

    return runFrameScanLoop(bytesToCheck - bytesInBuffer, bytesInBuffer, throwOnLimit);
  }

  private boolean runFrameScanLoop(int bytesToCheck, int bytesInBuffer, boolean throwOnLimit) throws IOException {
    while (bytesToCheck > 0) {
      for (int i = bytesInBuffer; i < scanBuffer.length && bytesToCheck > 0; i++, bytesToCheck--) {
        int next = inputStream.read();
        if (next == -1) {
          return false;
        }

        scanBuffer[i] = (byte) (next & 0xFF);

        if (parseFrameAt(i + 1)) {
          frameHeaderRead = true;
          return true;
        }
      }

      bytesInBuffer = copyScanBufferEndToBeginning();
    }

    if (throwOnLimit) {
      throw new IllegalStateException("Mp3 frame not found.");
    }

    return false;
  }

  private int copyScanBufferEndToBeginning() {
    for (int i = 0; i < HEADER_SIZE - 1; i++) {
      scanBuffer[i] = scanBuffer[scanBuffer.length - HEADER_SIZE + i + 1];
    }

    return HEADER_SIZE - 1;
  }

  private boolean parseFrameAt(int scanOffset) {
    if (scanOffset >= HEADER_SIZE && (frameSize = Mp3Decoder.getFrameSize(scanBuffer, scanOffset - HEADER_SIZE)) > 0) {
      for (int i = 0; i < HEADER_SIZE; i++) {
        frameBuffer[i] = scanBuffer[scanOffset - HEADER_SIZE + i];
      }

      frameBufferPosition = HEADER_SIZE;
      return true;
    }

    return false;
  }

  /**
   * Fills the buffer for the current frame. If no frame header has been read previously, it will first scan for the
   * sync bytes of the next frame in the stream.
   * @return False if EOF was encountered while looking for the next frame, true otherwise
   * @throws IOException On IO error
   */
  public boolean fillFrameBuffer() throws IOException {
    if (!frameHeaderRead && !scanForFrame(Integer.MAX_VALUE, true)) {
      return false;
    }

    dataInput.readFully(frameBuffer, frameBufferPosition, frameSize - frameBufferPosition);
    frameBufferPosition = frameSize;
    return true;
  }

  /**
   * Forget the current frame and make next calls look for the next frame.
   */
  public void nextFrame() {
    frameHeaderRead = false;
    frameBufferPosition = 0;
  }

  /**
   * @return The start position of the current frame in the stream.
   */
  public long getFrameStartPosition() {
    return inputStream.getPosition() - frameBufferPosition;
  }

  /**
   * @return Size of the current frame in bytes.
   */
  public int getFrameSize() {
    return frameSize;
  }

  /**
   * Append some bytes to the frame sync scan buffer. This must be called when some bytes have been read externally that
   * may actually be part of the next frame header.
   *
   * @param data The buffer to copy from
   * @param offset The offset in the buffer
   * @param length The length of the region to copy
   */
  public void appendToScanBuffer(byte[] data, int offset, int length) {
    System.arraycopy(data, offset, scanBuffer, 0, length);
    scanBufferPosition = length;
  }
}
