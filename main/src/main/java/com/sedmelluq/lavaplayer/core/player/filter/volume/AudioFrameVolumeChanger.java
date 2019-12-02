package com.sedmelluq.lavaplayer.core.player.filter.volume;

import com.sedmelluq.lavaplayer.core.format.transcoder.AudioChunkDecoder;
import com.sedmelluq.lavaplayer.core.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;
import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrame;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameRebuilder;
import com.sedmelluq.lavaplayer.core.player.frame.ImmutableAudioFrame;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * A frame rebuilder to apply a specific volume level to the frames.
 */
public class AudioFrameVolumeChanger implements AudioFrameRebuilder {
  private final AudioConfiguration configuration;
  private final AudioDataFormat format;
  private final int newVolume;
  private final ShortBuffer sampleBuffer;
  private final PcmVolumeProcessor volumeProcessor;

  private AudioChunkEncoder encoder;
  private AudioChunkDecoder decoder;
  private int frameIndex;

  private AudioFrameVolumeChanger(AudioConfiguration configuration, int newVolume) {
    this.configuration = configuration;
    this.format = configuration.getOutputFormat();
    this.newVolume = newVolume;

    this.sampleBuffer = ByteBuffer
        .allocateDirect(format.totalSampleCount() * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer();
    this.volumeProcessor = new PcmVolumeProcessor(100);
  }

  @Override
  public AudioFrame rebuild(AudioFrame frame) {
    if (frame.getVolume() == newVolume) {
      return frame;
    }

    decoder.decode(frame.getData(), sampleBuffer);

    int targetVolume = newVolume;

    if (++frameIndex < 50) {
      targetVolume = (int) ((newVolume - frame.getVolume()) * (frameIndex / 50.0) + frame.getVolume());
    }

    // Volume 0 is stored in the frame with volume 100 buffer
    if (targetVolume != 0) {
      volumeProcessor.applyVolume(frame.getVolume(), targetVolume, sampleBuffer);
    }

    byte[] bytes = encoder.encode(sampleBuffer);

    // One frame per 20ms is consumed. To not spike the CPU usage, reencode only once per 5ms. By the time the buffer is
    // fully rebuilt, it is probably near to 3/4 its maximum size.
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // Keep it interrupted, it will trip on the next interruptible operation
      Thread.currentThread().interrupt();
    }

    return new ImmutableAudioFrame(frame.getTimecode(), bytes, targetVolume, format);
  }

  private void setupLibraries() {
    encoder = format.createEncoder(configuration);
    decoder = format.createDecoder();
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
   * @param context Configuration and output information for processing
   */
  public static void apply(AudioPlaybackContext context) {
    AudioFrameVolumeChanger volumeChanger = new AudioFrameVolumeChanger(context.getConfiguration(),
        context.getConfiguration().getVolumeLevel());

    try {
      volumeChanger.setupLibraries();
      context.getFrameBuffer().rebuild(volumeChanger);
    } finally {
      volumeChanger.clearLibraries();
    }
  }
}
