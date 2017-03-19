package com.sedmelluq.discord.lavaplayer.natives.opus;

import com.sedmelluq.discord.lavaplayer.natives.NativeResourceHolder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * A wrapper around the native methods of OpusDecoderLibrary.
 */
public class OpusDecoder extends NativeResourceHolder {
  private final OpusDecoderLibrary library;
  private final long instance;
  private final int channels;

  /**
   * @param sampleRate Input sample rate
   * @param channels Channel count
   */
  public OpusDecoder(int sampleRate, int channels) {
    library = OpusDecoderLibrary.getInstance();
    instance = library.create(sampleRate, channels);
    this.channels = channels;

    if (instance == 0) {
      throw new IllegalStateException("Failed to create a decoder instance with sample rate " +
          sampleRate + " and channel count " + channels);
    }
  }

  /**
   * Encode the input buffer to output.
   * @param directInput Input byte buffer
   * @param directOutput Output sample buffer
   * @return Number of bytes written to the output
   */
  public int decode(ByteBuffer directInput, ShortBuffer directOutput) {
    checkNotReleased();

    if (!directInput.isDirect() || !directOutput.isDirect()) {
      throw new IllegalArgumentException("Arguments must be direct buffers.");
    }

    directOutput.clear();
    int result = library.decode(instance, directInput, directInput.remaining(), directOutput, directOutput.remaining() / channels);

    if (result < 0) {
      throw new IllegalStateException("Decoding failed with error " + result);
    }

    directOutput.position(result * channels);
    directOutput.flip();

    return result;
  }

  @Override
  protected void freeResources() {
    library.destroy(instance);
  }

  /**
   * Get the frame size from an opus packet
   * @param sampleRate The sample rate of the packet
   * @param buffer The buffer containing the packet
   * @param offset Packet offset in the buffer
   * @param length Packet length in the buffer
   * @return Frame size
   */
  public static int getPacketFrameSize(int sampleRate, byte[] buffer, int offset, int length) {
    if (length < 1) {
      return 0;
    }

    int frameCount = getPacketFrameCount(buffer, offset, length);
    if (frameCount < 0) {
      return 0;
    }

    int samples = frameCount * getPacketSamplesPerFrame(sampleRate, buffer[offset]);
    if (samples * 25 > sampleRate * 3) {
      return 0;
    }

    return samples;
  }

  private static int getPacketFrameCount(byte[] buffer, int offset, int length) {
    switch (buffer[offset] & 0x03) {
      case 0:
        return 1;
      case 3:
        return length < 2 ? -1 : buffer[offset + 1] & 0x3F;
      default:
        return 2;
    }
  }

  private static int getPacketSamplesPerFrame(int frequency, int firstByte) {
    int shiftBits = (firstByte >> 3) & 0x03;

    if ((firstByte & 0x80) != 0) {
      return (frequency << shiftBits) / 400;
    } else if ((firstByte & 0x60) == 0x60) {
      return (firstByte & 0x08) != 0 ? frequency / 50 : frequency / 100;
    } else if (shiftBits == 3) {
      return frequency * 60 / 1000;
    } else {
      return (frequency << shiftBits) / 100;
    }
  }
}
