package com.sedmelluq.lavaplayer.core.player.filter.volume;

import com.sedmelluq.lavaplayer.core.player.filter.AudioPostProcessor;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import java.nio.ShortBuffer;

/**
 * Audio chunk post processor to apply selected volume.
 */
public class VolumePostProcessor implements AudioPostProcessor {
  private final PcmVolumeProcessor volumeProcessor;
  private final AudioPlaybackContext context;

  /**
   * @param context Configuration and output information for processing
   */
  public VolumePostProcessor(AudioPlaybackContext context) {
    this.context = context;
    this.volumeProcessor = new PcmVolumeProcessor(context.getConfiguration().getVolumeLevel());
  }

  @Override
  public void process(long timecode, ShortBuffer buffer) throws InterruptedException {
    int currentVolume = context.getConfiguration().getVolumeLevel();

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
