package com.sedmelluq.discord.lavaplayer.filter.volume;

import com.sedmelluq.discord.lavaplayer.natives.opus.OpusDecoder;
import com.sedmelluq.discord.lavaplayer.natives.opus.OpusEncoder;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameRebuilder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static com.sedmelluq.discord.lavaplayer.filter.OpusEncodingPcmAudioFilter.CHANNEL_COUNT;
import static com.sedmelluq.discord.lavaplayer.filter.OpusEncodingPcmAudioFilter.FRAME_SIZE;
import static com.sedmelluq.discord.lavaplayer.filter.OpusEncodingPcmAudioFilter.FREQUENCY;

/**
 * A frame rebuilder to apply a specific volume level to the frames.
 */
public class AudioFrameVolumeChanger implements AudioFrameRebuilder {
  private final AudioConfiguration configuration;
  private final int newVolume;
  private final ByteBuffer encodedBuffer;
  private final ShortBuffer sampleBuffer;
  private final PcmVolumeProcessor volumeProcessor;

  private OpusEncoder encoder;
  private OpusDecoder decoder;
  private int frameIndex;

  private AudioFrameVolumeChanger(AudioConfiguration configuration, int newVolume) {
    this.configuration = configuration;
    this.newVolume = newVolume;

    this.encodedBuffer = ByteBuffer.allocateDirect(4096);
    this.sampleBuffer = ByteBuffer.allocateDirect(FRAME_SIZE * CHANNEL_COUNT * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    this.volumeProcessor = new PcmVolumeProcessor(100);
  }

  @Override
  public AudioFrame rebuild(AudioFrame frame) {
    if (frame.volume == newVolume) {
      return frame;
    }

    encodedBuffer.clear();
    encodedBuffer.put(frame.data);
    encodedBuffer.flip();

    sampleBuffer.clear();
    decoder.decode(encodedBuffer, sampleBuffer);

    encodedBuffer.clear();

    int targetVolume = newVolume;

    if (++frameIndex < 50) {
      targetVolume = (int) ((newVolume - frame.volume) * (frameIndex / 50.0) + frame.volume);
    }

    // Volume 0 is stored in the frame with volume 100 buffer
    if (targetVolume != 0) {
      volumeProcessor.applyVolume(frame.volume, targetVolume, sampleBuffer);
    }

    encoder.encode(sampleBuffer, FRAME_SIZE, encodedBuffer);

    byte[] bytes = new byte[encodedBuffer.remaining()];
    encodedBuffer.get(bytes);

    // One frame per 20ms is consumed. To not spike the CPU usage, reencode only once per 5ms. By the time the buffer is
    // fully rebuilt, it is probably near to 3/4 its maximum size.
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // Keep it interrupted, it will trip on the next interruptible operation
      Thread.currentThread().interrupt();
    }

    return new AudioFrame(frame.timecode, bytes, targetVolume);
  }

  private void setupLibraries() {
    encoder = new OpusEncoder(FREQUENCY, CHANNEL_COUNT, configuration.getOpusEncodingQuality());
    decoder = new OpusDecoder(FREQUENCY, CHANNEL_COUNT);
  }

  private void clearLibraries() {
    if (encoder != null) {
      encoder.close();
    }

    if (decoder != null) {
      decoder.close();
    }
  }

  /**
   * Applies a volume level to the buffered frames of a frame consumer
   * @param frameConsumer The frame consumer
   * @param newVolume New volume to apply
   */
  public static void apply(AudioConfiguration configuration, AudioFrameConsumer frameConsumer, int newVolume) {
    AudioFrameVolumeChanger volumeChanger = new AudioFrameVolumeChanger(configuration, newVolume);

    try {
      volumeChanger.setupLibraries();
      frameConsumer.rebuild(volumeChanger);
    } finally {
      volumeChanger.clearLibraries();
    }
  }
}
