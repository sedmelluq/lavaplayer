package com.sedmelluq.lavaplayer.core.player.configuration;

import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;
import com.sedmelluq.lavaplayer.core.format.StandardAudioDataFormats;
import com.sedmelluq.lavaplayer.core.player.filter.PcmFilterFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RootAudioConfiguration implements MutableAudioConfiguration {
  public static final int OPUS_QUALITY_MAX = 10;

  private volatile int volumeLevel = 100;
  private volatile long frameBufferDuration = (int) TimeUnit.SECONDS.toMillis(5);
  private volatile boolean filterHotSwapEnabled = false;
  private volatile PcmFilterFactory filterFactory = null;
  private volatile ResamplingQuality resamplingQuality = ResamplingQuality.LOW;
  private volatile AudioDataFormat outputFormat = StandardAudioDataFormats.DISCORD_OPUS;
  private volatile int opusEncodingQuality = OPUS_QUALITY_MAX;
  private volatile boolean usingSeekGhosting = true;
  private volatile long trackStuckThreshold = 10000;
  private volatile long trackCleanupThreshold = TimeUnit.MINUTES.toMillis(1);
  private final Map<String, Object> customOptions = new ConcurrentHashMap<>();

  @Override
  public void setVolumeLevel(int volumeLevel) {
    this.volumeLevel = volumeLevel;
  }

  @Override
  public void setFrameBufferDuration(long duration) {
    this.frameBufferDuration = duration;
  }

  @Override
  public void setFilterHotSwapEnabled(boolean enabled) {
    this.filterHotSwapEnabled = enabled;
  }

  @Override
  public void setFilterFactory(PcmFilterFactory filterFactory) {
    this.filterFactory = filterFactory;
  }

  @Override
  public void setResamplingQuality(ResamplingQuality quality) {
    this.resamplingQuality = quality;
  }

  @Override
  public void setOutputFormat(AudioDataFormat format) {
    this.outputFormat = format;
  }

  @Override
  public void setOpusEncodingQuality(int quality) {
    this.opusEncodingQuality = Math.max(0, Math.min(quality, OPUS_QUALITY_MAX));
  }

  @Override
  public void setUsingSeekGhosting(boolean enabled) {
    this.usingSeekGhosting = enabled;
  }

  @Override
  public void setTrackStuckThreshold(long threshold) {
    this.trackStuckThreshold = threshold;
  }

  @Override
  public void setTrackCleanupThreshold(long threshold) {
    this.trackCleanupThreshold = threshold;
  }

  @Override
  public void setCustomOption(String name, Object value) {
    customOptions.put(name, value);
  }

  @Override
  public int getVolumeLevel() {
    return volumeLevel;
  }

  @Override
  public long getFrameBufferDuration() {
    return frameBufferDuration;
  }

  @Override
  public boolean isFilterHotSwapEnabled() {
    return filterHotSwapEnabled;
  }

  @Override
  public PcmFilterFactory getFilterFactory() {
    return filterFactory;
  }

  @Override
  public ResamplingQuality getResamplingQuality() {
    return resamplingQuality;
  }

  @Override
  public AudioDataFormat getOutputFormat() {
    return outputFormat;
  }

  @Override
  public int getOpusEncodingQuality() {
    return opusEncodingQuality;
  }

  @Override
  public boolean isUsingSeekGhosting() {
    return usingSeekGhosting;
  }

  @Override
  public long getTrackStuckThreshold() {
    return trackStuckThreshold;
  }

  @Override
  public long getTrackCleanupThreshold() {
    return trackCleanupThreshold;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getCustomOption(String name, Class<T> klass) {
    Object value = customOptions.get(name);

    if (klass.isInstance(value)) {
      return (T) value;
    }

    return null;
  }
}
