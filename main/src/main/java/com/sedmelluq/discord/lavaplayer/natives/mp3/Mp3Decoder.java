package com.sedmelluq.discord.lavaplayer.natives.mp3;

import com.sedmelluq.discord.lavaplayer.natives.NativeResourceHolder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * A wrapper around the native methods of OpusDecoderLibrary.
 */
public class Mp3Decoder extends NativeResourceHolder {
  public static final int SAMPLES_PER_FRAME = 1152;
  public static final int HEADER_SIZE = 4;

  private final Mp3DecoderLibrary library;
  private final long instance;

  /**
   * Create a new instance of mp3 decoder
   */
  public Mp3Decoder() {
    library = Mp3DecoderLibrary.getInstance();
    instance = library.create();

    if (instance == 0) {
      throw new IllegalStateException("Failed to create a decoder instance");
    }
  }

  /**
   * Encode the input buffer to output.
   * @param directInput Input byte buffer
   * @param directOutput Output sample buffer
   * @return Number of samples written to the output
   */
  public int decode(ByteBuffer directInput, ShortBuffer directOutput) {
    checkNotReleased();

    if (!directInput.isDirect() || !directOutput.isDirect()) {
      throw new IllegalArgumentException("Arguments must be direct buffers.");
    }

    directOutput.clear();
    int result = library.decode(instance, directInput, directInput.remaining(), directOutput, directOutput.remaining() * 2);

    if (result == -10 || result == -11) {
      result = 0;
    } else if (result < 0) {
      throw new IllegalStateException("Decoding failed with error " + result);
    }

    directOutput.position(result / 2);
    directOutput.flip();

    return result / 2;
  }

  @Override
  protected void freeResources() {
    library.destroy(instance);
  }

  private static int getFrameBitRate(byte[] buffer, int offset) {
    switch ((buffer[offset + 2] & 0xF0) >>> 4) {
      case 1: return 32000;
      case 2: return 40000;
      case 3: return 48000;
      case 4: return 56000;
      case 5: return 64000;
      case 6: return 80000;
      case 7: return 96000;
      case 8: return 112000;
      case 9: return 128000;
      case 10: return 160000;
      case 11: return 192000;
      case 12: return 224000;
      case 13: return 256000;
      case 14: return 320000;
      default:
        throw new IllegalArgumentException("Not valid bitrate");
    }
  }

  private static int calculateFrameSize(int bitRate, int sampleRate, boolean hasPadding) {
    return 144 * bitRate / sampleRate + (hasPadding ? 1 : 0);
  }

  /**
   * Get the sample rate for the current frame
   * @param buffer Buffer which contains the frame header
   * @param offset Offset to the frame header
   * @return Sample rate
   */
  public static int getFrameSampleRate(byte[] buffer, int offset) {
    switch ((buffer[offset + 2] & 0x0C) >>> 2) {
      case 0: return 44100;
      case 1: return 48000;
      case 2: return 32000;
      default:
        throw new IllegalArgumentException("Not valid sample rate");
    }
  }

  /**
   * Get the frame size of the specified 4 bytes
   * @param buffer Buffer which contains the frame header
   * @param offset Offset to the frame header
   * @return Frame size, or zero if not a valid frame header
   */
  public static int getFrameSize(byte[] buffer, int offset) {
    int first = buffer[offset] & 0xFF;
    int second = buffer[offset + 1] & 0xFF;
    int third = buffer[offset + 2] & 0xFF;

    boolean invalid = (first != 0xFF || (second & 0xE0) != 0xE0) // Frame sync does not match
        || (second & 0x18) != 0x18 // Not MPEG-2, not dealing with this stuff
        || (second & 0x06) != 0x02 // Not Layer III, not dealing with this stuff
        || (third & 0xF0) == 0x00 // No defined bitrate
        || (third & 0xF0) == 0xF0 // Invalid bitrate
        || (third & 0x0C) == 0x0C // Invalid sampling rate
        || (buffer[offset + 3] & 0x80) == 0x80; // Not dealing with mono

    if (invalid) {
      return 0;
    }

    int bitRate = getFrameBitRate(buffer, offset);
    int sampleRate = getFrameSampleRate(buffer, offset);
    boolean hasPadding = (third & 0x02) != 0;

    return calculateFrameSize(bitRate, sampleRate, hasPadding);
  }

  /**
   * Get the average frame size based on this frame
   * @param buffer Buffer which contains the frame header
   * @param offset Offset to the frame header
   * @return Average frame size, assuming CBR
   */
  public static double getAverageFrameSize(byte[] buffer, int offset) {
    int bitRate = getFrameBitRate(buffer, offset);
    int sampleRate = getFrameSampleRate(buffer, offset);

    return 144.0 * bitRate / sampleRate;
  }

  public static int getMaximumFrameSize() {
    return calculateFrameSize(320000, 32000, true);
  }
}
