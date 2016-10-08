package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.filter.volume.PcmVolumeProcessor;
import com.sedmelluq.discord.lavaplayer.natives.opus.OpusEncoder;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;
import com.sedmelluq.discord.lavaplayer.filter.volume.AudioFrameVolumeChanger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encodes the input audio samples to OPUS frames and passes them to a frame consumer.
 */
public class OpusEncodingPcmAudioFilter implements FloatPcmAudioFilter, ShortPcmAudioFilter {
  private static final Logger log = LoggerFactory.getLogger(OpusEncodingPcmAudioFilter.class);
  private static final short[] zeroPadding = new short[128];

  public static final int FREQUENCY = 48000;
  public static final int CHANNEL_COUNT = 2;
  public static final int FRAME_SIZE = 960;

  private static final int CHUNK_LENGTH_MS = 20;
  private static final int SAMPLES_PER_MS = 48;

  private final AudioConfiguration configuration;
  private final AudioFrameConsumer frameConsumer;
  private final ShortBuffer frameBuffer;
  private final ByteBuffer encoded;
  private final OpusEncoder opusEncoder;
  private final AtomicInteger volumeLevel;
  private final PcmVolumeProcessor volumeProcessor;

  private long ignoredFrames;
  private long nextTimecode;

  /**
   * @param frameConsumer Frame consumer where to pass the encoded frames to
   * @param volumeLevel Mutable volume level
   */
  public OpusEncodingPcmAudioFilter(AudioConfiguration configuration, AudioFrameConsumer frameConsumer, AtomicInteger volumeLevel) {
    this.configuration = configuration;
    this.frameConsumer = frameConsumer;
    this.frameBuffer = ByteBuffer.allocateDirect(CHUNK_LENGTH_MS * SAMPLES_PER_MS * CHANNEL_COUNT * 2).
        order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    this.encoded = ByteBuffer.allocateDirect(4096);
    this.volumeLevel = volumeLevel;
    this.volumeProcessor = new PcmVolumeProcessor(volumeLevel.get());

    opusEncoder = new OpusEncoder(FREQUENCY, CHANNEL_COUNT, configuration.getOpusEncodingQuality());
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
      int currentVolume = volumeLevel.get();

      if (currentVolume != volumeProcessor.getLastVolume()) {
        AudioFrameVolumeChanger.apply(configuration, frameConsumer, currentVolume);
      }

      frameBuffer.clear();

      // Volume 0 is stored in the frame with volume 100 buffer
      if (currentVolume != 0) {
        volumeProcessor.applyVolume(100, currentVolume, frameBuffer);
      } else {
        volumeProcessor.setLastVolume(0);
      }

      encoded.clear();

      int encodedLength = opusEncoder.encode(frameBuffer, FRAME_SIZE, encoded);

      byte[] encodedBytes = new byte[encodedLength];
      encoded.get(encodedBytes);

      frameConsumer.consume(new AudioFrame(nextTimecode, encodedBytes, currentVolume));
      frameBuffer.clear();

      nextTimecode += CHUNK_LENGTH_MS;
    }
  }
}
