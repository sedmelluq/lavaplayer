package com.sedmelluq.discord.lavaplayer.filter.volume;

import com.sedmelluq.discord.lavaplayer.filter.AudioPostProcessor;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.nio.ShortBuffer;

/**
 * Audio chunk post processor to apply selected volume.
 */
public class VolumePostProcessor implements AudioPostProcessor {
  private final PcmVolumeProcessor volumeProcessor;
  private final AudioProcessingContext context;

  /**
   * @param context Configuration and output information for processing
   */
  public VolumePostProcessor(AudioProcessingContext context) {
    this.context = context;
    this.volumeProcessor = new PcmVolumeProcessor(context.playerOptions.volumeLevel.get());
  }

  @Override
  public void process(long timecode, ShortBuffer buffer) throws InterruptedException {
    int currentVolume = context.playerOptions.volumeLevel.get();

    if (currentVolume != volumeProcessor.getLastVolume()) {
      AudioFrameVolumeChanger.apply(context);
    }

    // Volume 0 is stored in the frame with volume 100 buffer
    if (currentVolume != 0) {
      volumeProcessor.applyVolume(100, currentVolume, buffer);
    } else {
      volumeProcessor.setLastVolume(0);
    }
  }

  @Override
  public void close() {
    // Nothing to close here
  }
}
