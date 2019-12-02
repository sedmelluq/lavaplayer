package com.sedmelluq.lavaplayer.core.player.configuration;

import com.sedmelluq.lavaplayer.core.player.filter.PcmFilterFactory;
import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;

public interface MutableAudioConfiguration extends AudioConfiguration {
  void setVolumeLevel(int volumeLevel);

  void setFrameBufferDuration(long duration);

  void setFilterHotSwapEnabled(boolean enabled);

  void setFilterFactory(PcmFilterFactory filterFactory);

  void setResamplingQuality(ResamplingQuality quality);

  void setOutputFormat(AudioDataFormat format);

  void setOpusEncodingQuality(int quality);

  void setUsingSeekGhosting(boolean enabled);

  void setTrackStuckThreshold(long threshold);

  void setTrackCleanupThreshold(long threshold);

  void setCustomOption(String name, Object value);
}
