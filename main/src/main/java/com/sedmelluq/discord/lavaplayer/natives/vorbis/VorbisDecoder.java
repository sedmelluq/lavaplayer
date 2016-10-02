package com.sedmelluq.discord.lavaplayer.natives.vorbis;

import com.sedmelluq.discord.lavaplayer.natives.NativeResourceHolder;

import java.nio.ByteBuffer;

/**
 * A wrapper around the native methods of AacDecoder, which uses libvorbis native library.
 */
public class VorbisDecoder extends NativeResourceHolder {
  private final VorbisDecoderLibrary library;
  private final long instance;
  private int channelCount = 0;

  /**
   * Create an instance.
   */
  public VorbisDecoder() {
    library = VorbisDecoderLibrary.getInstance();
    instance = library.create();
  }

  /**
   * Parse one header of a vorbis stream.
   *
   * @param buffer Buffer containing the header
   * @param length Length of the header in the buffer
   * @param isBeginning Whether this is the first header
   */
  public void parseHeader(ByteBuffer buffer, int length, boolean isBeginning) {
    checkNotReleased();

    if (!buffer.isDirect()) {
      throw new IllegalArgumentException("Buffer argument must be a direct buffer.");
    } else if (buffer.remaining() < length) {
      throw new IllegalArgumentException("Cannot take more from buffer than available.");
    }

    int error = library.processHeader(instance, buffer, buffer.position(), length, isBeginning);
    buffer.position(buffer.position() + length);

    if (error != 0) {
      throw new IllegalStateException("Processing header failed with error " + error + ".");
    }
  }

  /**
   * Initialise the decoder, headers must already be processed.
   */
  public void initialise() {
    checkNotReleased();

    if (!library.initialise(instance)) {
      throw new IllegalStateException("Could not initialise library.");
    }

    channelCount = library.getChannelCount(instance);
  }

  /**
   * Get the number of channels, valid only after initialisation.
   * @return Number of channels
   */
  public int getChannelCount() {
    return channelCount;
  }

  /**
   * Provide input for the decoder
   * @param buffer Buffer with the input
   */
  public void input(ByteBuffer buffer) {
    checkNotReleased();

    if (!buffer.isDirect()) {
      throw new IllegalArgumentException("Buffer argument must be a direct buffer.");
    }

    int result = library.input(instance, buffer, buffer.position(), buffer.remaining());
    buffer.position(buffer.position() + buffer.remaining());

    if (result != 0) {
      throw new IllegalStateException("Passing input failed with error " + result + ".");
    }
  }

  /**
   * Fetch output from the decoder
   * @param channels Channel buffers to fetch the output to
   * @return The number of samples fetched for each channel
   */
  public int output(float[][] channels) {
    checkNotReleased();

    if (channels.length != channelCount) {
      throw new IllegalStateException("Invalid channel float buffer length");
    }

    int result = library.output(instance, channels, channels[0].length);
    if (result < 0) {
      throw new IllegalStateException("Retrieving output failed");
    }

    return result;
  }

  @Override
  protected void freeResources() {
    library.destroy(instance);
  }
}
