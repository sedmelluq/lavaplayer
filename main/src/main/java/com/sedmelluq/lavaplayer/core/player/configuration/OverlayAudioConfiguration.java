package com.sedmelluq.lavaplayer.core.player.configuration;

public interface OverlayAudioConfiguration extends MutableAudioConfiguration {
  boolean isOverlaid(Field field);

  boolean isOverlaid(String customOptionName);

  void discardOverlay(Field field);

  void discardOverlay(String customOptionName);

  void discardAllOverlays();

  enum Field {
    VOLUME_LEVEL,
    FRAME_BUFFER_DURATION,
    FILTER_HOT_SWAP_ENABLED,
    FILTER_FACTORY,
    RESAMPLING_QUALITY,
    OUTPUT_FORMAT,
    OPUS_ENCODING_QUALITY,
    USING_SEEK_GHOSTING,
    TRACK_STUCK_THRESHOLD,
    TRACK_CLEANUP_THRESHOLD
  }
}
