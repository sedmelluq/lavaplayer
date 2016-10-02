package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.natives.opus.OpusEncoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Encodes the input audio samples to OPUS frames and passes them to a frame consumer.
 */
public class OpusEncodingPcmAudioFilter implements FloatPcmAudioFilter, ShortPcmAudioFilter {
  private static final Logger log = LoggerFactory.getLogger(OpusEncodingPcmAudioFilter.class);
  private static final short[] zeroPadding = new short[128];

  public static final int FREQUENCY = 48000;
  public static final int CHANNEL_COUNT = 2;

  private static final int CHUNK_LENGTH_MS = 20;
  private static final int SAMPLES_PER_MS = 48;

  private final AudioFrameConsumer frameConsumer;
  private final ShortBuffer frameBuffer;
  private final ByteBuffer encoded;
  private final OpusEncoder opusEncoder;

  private long ignoredFrames;
  private long nextTimecode;

  /**
   * @param frameConsumer Frame consumer where to pass the encoded frames to
   */
  public OpusEncodingPcmAudioFilter(AudioFrameConsumer frameConsumer) {
    this.frameConsumer = frameConsumer;
    this.frameBuffer = ByteBuffer.allocateDirect(CHUNK_LENGTH_MS * SAMPLES_PER_MS * CHANNEL_COUNT * 2).
        order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    this.encoded = ByteBuffer.allocateDirect(4096);

    opusEncoder = new OpusEncoder(48000, 2);
    nextTimecode = 0;
  }

  private short decodeSample(float sample) {
    return (short) Math.min(Math.max((int)(sample * 32768.f), -32768), 32767);
  }

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    frameBuffer.clear();
    ignoredFrames = requestedTime > providedTime ? (requestedTime - providedTime) * SAMPLES_PER_MS * CHANNEL_COUNT : 0;
    nextTimecode = Math.max(requestedTime, providedTime);

    if (ignoredFrames > 0) {
      log.debug("Ignoring {} frames due to inaccurate seek (requested {}, provided {}).", ignoredFrames, requestedTime, providedTime);
    }
  }

  @Override
  public void flush() throws InterruptedException {
    if (frameBuffer.position() > 0) {
      fillFrameBuffer();
      dispatch();
    }
  }

  @Override
  public void close() {
    opusEncoder.close();
  }

  private void fillFrameBuffer() {
    while (frameBuffer.remaining() >= zeroPadding.length) {
      frameBuffer.put(zeroPadding);
    }

    while (frameBuffer.remaining() > 0) {
      frameBuffer.put((short) 0);
    }
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    for (int i = 0; i < length; i++) {
      if (ignoredFrames > 0) {
        ignoredFrames--;
      } else {
        frameBuffer.put(input[offset + i]);

        dispatch();
      }
    }
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    ShortBuffer local = buffer.duplicate();

    while (buffer.remaining() > 0) {
      if (ignoredFrames > 0) {
        ignoredFrames--;
      } else {
        int chunk = Math.min(buffer.remaining(), frameBuffer.remaining());
        local.position(buffer.position());
        local.limit(local.position() + chunk);

        frameBuffer.put(local);
        dispatch();

        buffer.position(buffer.position() + chunk);
      }
    }
  }

  @Override
  public void process(float[][] buffer, int offset, int length) throws InterruptedException {
    int secondChannelIndex = Math.min(1, buffer.length - 1);

    for (int i = 0; i < length; i++) {
      if (ignoredFrames > 0) {
        ignoredFrames--;
      } else {
        frameBuffer.put(decodeSample(buffer[0][offset + i]));
        frameBuffer.put(decodeSample(buffer[secondChannelIndex][offset + i]));

        dispatch();
      }
    }
  }

  private void dispatch() throws InterruptedException {
    if (!frameBuffer.hasRemaining()) {
      frameBuffer.clear();

      encoded.clear();
      int encodedLength = opusEncoder.encode(frameBuffer, 960, encoded);

      byte[] encodedBytes = new byte[encodedLength];
      encoded.get(encodedBytes);

      frameConsumer.consume(new AudioFrame(nextTimecode, encodedBytes));
      frameBuffer.clear();

      nextTimecode += CHUNK_LENGTH_MS;
    }
  }
}
