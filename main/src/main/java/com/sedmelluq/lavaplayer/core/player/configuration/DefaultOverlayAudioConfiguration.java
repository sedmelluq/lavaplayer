package com.sedmelluq.lavaplayer.core.player.configuration;

import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;
import com.sedmelluq.lavaplayer.core.player.filter.PcmFilterFactory;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class DefaultOverlayAudioConfiguration implements OverlayAudioConfiguration {
  private final AudioConfiguration parent;

  private final AtomicReferenceArray<Object> values =
      new AtomicReferenceArray<>(Field.class.getEnumConstants().length);

  private final Map<String, Optional<Object>> customOptions = new ConcurrentHashMap<>();

  public DefaultOverlayAudioConfiguration(AudioConfiguration parent) {
    this.parent = parent;
  }

  @Override
  public void setVolumeLevel(int volumeLevel) {
    set(Field.VOLUME_LEVEL, volumeLevel);
  }

  @Override
  public void setFrameBufferDuration(long duration) {
    set(Field.FRAME_BUFFER_DURATION, duration);
  }

  @Override
  public void setFilterHotSwapEnabled(boolean enabled) {
    set(Field.FILTER_HOT_SWAP_ENABLED, enabled);
  }

  @Override
  public void setFilterFactory(PcmFilterFactory filterFactory) {
    set(Field.FILTER_FACTORY, Optional.ofNullable(filterFactory));
  }

  @Override
  public void setResamplingQuality(ResamplingQuality quality) {
    set(Field.RESAMPLING_QUALITY, quality);
  }

  @Override
  public void setOutputFormat(AudioDataFormat format) {
    set(Field.OUTPUT_FORMAT, format);
  }

  @Override
  public void setOpusEncodingQuality(int quality) {
    set(Field.OPUS_ENCODING_QUALITY, quality);
  }

  @Override
  public void setUsingSeekGhosting(boolean enabled) {
    set(Field.USING_SEEK_GHOSTING, enabled);
  }

  @Override
  public void setTrackStuckThreshold(long threshold) {
    set(Field.TRACK_STUCK_THRESHOLD, threshold);
  }

  @Override
  public void setTrackCleanupThreshold(long threshold) {
    set(Field.TRACK_CLEANUP_THRESHOLD, threshold);
  }

  @Override
  public void setCustomOption(String name, Object value) {
    customOptions.put(name, Optional.ofNullable(value));
  }

  @Override
  public int getVolumeLevel() {
    Integer overlay = get(Field.VOLUME_LEVEL);
    return overlay != null ? overlay : parent.getVolumeLevel();
  }

  @Override
  public long getFrameBufferDuration() {
    Long overlay = get(Field.FRAME_BUFFER_DURATION);
    return overlay != null ? overlay : parent.getFrameBufferDuration();
  }

  @Override
  public boolean isFilterHotSwapEnabled() {
    Boolean overlay = get(Field.FILTER_HOT_SWAP_ENABLED);
    return overlay != null ? overlay : parent.isFilterHotSwapEnabled();
  }

  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public PcmFilterFactory getFilterFactory() {
    Optional<PcmFilterFactory> overlay = get(Field.FILTER_FACTORY);
    return overlay != null ? overlay.orElse(null) : parent.getFilterFactory();
  }

  @Override
  public ResamplingQuality getResamplingQuality() {
    ResamplingQuality overlay = get(Field.RESAMPLING_QUALITY);
    return overlay != null ? overlay : parent.getResamplingQuality();
  }

  @Override
  public AudioDataFormat getOutputFormat() {
    AudioDataFormat overlay = get(Field.OUTPUT_FORMAT);
    return overlay != null ? overlay : parent.getOutputFormat();
  }

  @Override
  public int getOpusEncodingQuality() {
    Integer overlay = get(Field.OPUS_ENCODING_QUALITY);
    return overlay != null ? overlay : parent.getOpusEncodingQuality();
  }

  @Override
  public boolean isUsingSeekGhosting() {
    Boolean overlay = get(Field.USING_SEEK_GHOSTING);
    return overlay != null ? overlay : parent.isUsingSeekGhosting();
  }

  @Override
  public long getTrackStuckThreshold() {
    Long overlay = get(Field.TRACK_STUCK_THRESHOLD);
    return overlay != null ? overlay : parent.getTrackStuckThreshold();
  }

  @Override
  public long getTrackCleanupThreshold() {
    Long overlay = get(Field.TRACK_CLEANUP_THRESHOLD);
    return overlay != null ? overlay : parent.getTrackCleanupThreshold();
  }

  @Override
  @SuppressWarnings({"unchecked", "OptionalAssignedToNull"})
  public <T> T getCustomOption(String name, Class<T> klass) {
    Optional<Object> holder = customOptions.get(name);

    if (holder == null) {
      return parent.getCustomOption(name, klass);
    } else if (holder.isPresent()) {
      Object value = holder.get();

      if (klass.isInstance(value)) {
        return (T) value;
      }
    }

    return null;
  }

  @Override
  public boolean isOverlaid(Field field) {
    return false;
  }

  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public boolean isOverlaid(String customOptionName) {
    return customOptions.get(customOptionName) != null;
  }

  @Override
  public void discardOverlay(Field field) {
    set(field, null);
  }

  @Override
  public void discardOverlay(String customOptionName) {
    customOptions.remove(customOptionName);
  }

  @Override
  public void discardAllOverlays() {
    for (Field field : Field.class.getEnumConstants()) {
      discardOverlay(field);
    }

    customOptions.clear();
  }

  private void set(Field field, Object value) {
    values.set(field.ordinal(), value);
  }

  @SuppressWarnings("unchecked")
  private <T> T get(Field field) {
    return (T) values.get(field.ordinal());
  }
}
