package com.sedmelluq.discord.lavaplayer.natives.aac;

import com.sedmelluq.discord.lavaplayer.natives.NativeResourceHolder;
import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamWriter;
import com.sedmelluq.discord.lavaplayer.tools.io.ByteBufferOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * A wrapper around the native methods of AacDecoder, which uses fdk-aac native library. Supports data with no transport
 * layer. The only AAC type verified to work with this is AAC_LC.
 */
public class AacDecoder extends NativeResourceHolder {
  private static final int TRANSPORT_NONE = 0;

  public static final int AAC_LC = 2;

  private final AacDecoderLibrary library;
  private final long instance;

  /**
   * Create a new decoder.
   */
  public AacDecoder() {
    library = AacDecoderLibrary.getInstance();
    instance = library.create(TRANSPORT_NONE);
  }

  /**
   * Configure the decoder. Must be called before the first decoding.
   *
   * @param objectType Audio object type as defined for Audio Specific Config: https://wiki.multimedia.cx/index.php?title=MPEG-4_Audio
   * @param frequency Frequency of samples in Hz
   * @param channels Number of channels.
   * @throws IllegalStateException If the decoder has already been closed.
   */
  public void configure(int objectType, int frequency, int channels) {
    long buffer = encodeConfiguration(objectType, frequency, channels);

    configureRaw(buffer);
  }

  /**
   * Configure the decoder. Must be called before the first decoding.
   *
   * @param config Raw ASC format configuration
   * @throws IllegalStateException If the decoder has already been closed.
   */
  public void configure(byte[] config) {
    if (config.length > 8) {
      throw new IllegalArgumentException("Cannot process a header larger than size 8");
    }

    long buffer = 0;
    for (int i = 0; i < config.length; i++) {
      buffer |= config[i] << (i << 3);
    }

    configureRaw(buffer);
  }

  private synchronized void configureRaw(long buffer) {
    checkNotReleased();

    int error;
    if ((error = library.configure(instance, buffer)) != 0) {
      throw new IllegalStateException("Configuring failed with error " + error);
    }
  }

  private static long encodeConfiguration(int objectType, int frequency, int channels) {
    try {
      ByteBuffer buffer = ByteBuffer.allocate(8);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      BitStreamWriter bitWriter = new BitStreamWriter(new ByteBufferOutputStream(buffer));

      bitWriter.write(objectType, 5);

      int frequencyIndex = getFrequencyIndex(frequency);
      bitWriter.write(frequencyIndex, 4);

      if (frequencyIndex == 15) {
        bitWriter.write(frequency, 24);
      }

      bitWriter.write(channels, 4);
      bitWriter.flush();

      buffer.clear();

      return buffer.getLong();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static int getFrequencyIndex(int frequency) {
    switch (frequency) {
      case 96000: return 0;
      case 88200: return 1;
      case 64000: return 2;
      case 48000: return 3;
      case 44100: return 4;
      case 32000: return 5;
      case 24000: return 6;
      case 22050: return 7;
      case 16000: return 8;
      case 12000: return 9;
      case 11025: return 10;
      case 8000: return 11;
      case 7350: return 12;
      default: return 15;
    }
  }

  /**
   * Fill the internal decoding buffer with the bytes from the buffer. May consume less bytes than the buffer provides.
   *
   * @param buffer DirectBuffer which contains the bytes to be added. Position and limit are respected and position is
   *               updated as a result of this operation.
   * @return The number of bytes consumed from the provided buffer.
   *
   * @throws IllegalArgumentException If the buffer is not a DirectBuffer.
   * @throws IllegalStateException If the decoder has already been closed.
   */
  public synchronized int fill(ByteBuffer buffer) {
    checkNotReleased();

    if (!buffer.isDirect()) {
      throw new IllegalArgumentException("Buffer argument must be a direct buffer.");
    }

    int readBytes = library.fill(instance, buffer, buffer.position(), buffer.limit());
    if (readBytes < 0) {
      throw new IllegalStateException("Filling decoder failed with error " + (-readBytes));
    }

    buffer.position(buffer.position() + readBytes);
    return readBytes;
  }

  /**
   * Decode a frame of audio into the given buffer. The caller is expected to know the frame size (with AAC-LC it is
   * always 1024 * channel count * Short.BYTES) and the buffer capacity must be at least of that size. Buffer position
   * and limit are ignored and not updated. Returns true if the frame buffer was filled, false if there was not enough
   * input for that, and throws an IllegalStateException for any other error.
   *
   * @param buffer DirectBuffer of signed PCM samples where the decoded frame will be stored. The caller is expected to
   *               know the frame size (with AAC-LC it is always 1024 * channel count * Short.BYTES) and the buffer
   *               capacity must be at least of that size. Buffer position and limit are ignored and not updated.
   * @param flush Whether all the buffered data should be flushed, set to true if no more input is expected.
   * @return True if the frame buffer was filled, false if there was not enough input for decoding a full frame.
   *
   * @throws IllegalArgumentException If the buffer is not a DirectBuffer.
   * @throws IllegalStateException If the decoding library returns an error other than running out of input data.
   * @throws IllegalStateException If the decoder has already been closed.
   */
  public synchronized boolean decode(ShortBuffer buffer, boolean flush) {
    checkNotReleased();

    if (!buffer.isDirect()) {
      throw new IllegalArgumentException("Buffer argument must be a direct buffer.");
    }

    int result = library.decode(instance, buffer, buffer.capacity(), flush);
    if (result != 0 && result != 4098) {
      throw new IllegalStateException("Error from decoder " + result);
    }

    return result == 0;
  }

  @Override
  protected void freeResources() {
    library.destroy(instance);
  }
}
