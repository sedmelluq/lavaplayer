package com.sedmelluq.discord.lavaplayer.natives.opus;

import com.sedmelluq.discord.lavaplayer.natives.NativeResourceHolder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * A wrapper around the native methods of OpusEncoderLibrary.
 */
public class OpusEncoder extends NativeResourceHolder {
  private final OpusEncoderLibrary library;
  private final long instance;

  /**
   * @param sampleRate Input sample rate
   * @param channels Channel count
   * @param quality Encoding quality (0-10)
   */
  public OpusEncoder(int sampleRate, int channels, int quality) {
    library = OpusEncoderLibrary.getInstance();
    instance = library.create(sampleRate, channels, OpusEncoderLibrary.APPLICATION_AUDIO, quality);

    if (instance == 0) {
      throw new IllegalStateException("Failed to create an encoder instance");
    }
  }

  /**
   * Encode the input buffer to output.
   * @param directInput Input sample buffer
   * @param frameSize Number of samples per channel
   * @param directOutput Output byte buffer
   * @return Number of bytes written to the output
   */
  public int encode(ShortBuffer directInput, int frameSize, ByteBuffer directOutput) {
    checkNotReleased();

    if (!directInput.isDirect() || !directOutput.isDirect()) {
      throw new IllegalArgumentException("Arguments must be direct buffers.");
    }

    directOutput.clear();
    int result = library.encode(instance, directInput, frameSize, directOutput, directOutput.capacity());

    if (result < 0) {
      throw new IllegalStateException("Encoding failed with error " + result);
    }

    directOutput.position(result);
    directOutput.flip();

    return result;
  }

  @Override
  protected void freeResources() {
    library.destroy(instance);
  }
}
