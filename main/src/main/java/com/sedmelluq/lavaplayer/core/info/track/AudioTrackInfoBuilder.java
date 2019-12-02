package com.sedmelluq.lavaplayer.core.info.track;

import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.METADATA_CORE;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.METADATA_EXTENDED;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.PLAYBACK_CACHE;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.PLAYBACK_CORE;

public class AudioTrackInfoBuilder {
  private final Map<String, AudioTrackProperty> properties;
  private final int flagMask;

  public static AudioTrackInfoBuilder fromTemplate(AudioTrackInfoTemplate template) {
    if (template == null) {
      return new AudioTrackInfoBuilder();
    }

    AudioTrackInfoBuilder builder = new AudioTrackInfoBuilder(template.getPropertyFlagMask());

    for (AudioTrackProperty property : template.getProperties()) {
      builder.with(property);
    }

    return builder;
  }

  public AudioTrackInfoBuilder() {
    this(PLAYBACK_CORE.mask | PLAYBACK_CACHE.mask | METADATA_CORE.mask | METADATA_EXTENDED.mask);
  }

  public AudioTrackInfoBuilder(int flagMask) {
    this.properties = new HashMap<>();
    this.flagMask = flagMask;
  }

  public AudioTrackInfoBuilder withProvider(AudioTrackInfoProvider provider) {
    provider.provideTrackInfo(this);
    return this;
  }

  public AudioTrackInfoBuilder with(AudioTrackProperty property) {
    if (property != null) {
      storeProperty(property);
    }

    return this;
  }

  public AudioTrackInfo build(Function<Map<String, AudioTrackProperty>, AudioTrackInfo> instantiator) {
    return instantiator.apply(properties);
  }

  public AudioTrackInfo build() {
    return new CoreAudioTrackInfo(properties);
  }

  private void storeProperty(AudioTrackProperty property) {
    if ((flagMask & property.getFlags()) != 0) {
      AudioTrackProperty existing = properties.get(property.getName());

      if (existing == null || existing.getPriority() <= property.getPriority()) {
        properties.put(property.getName(), property);
      }
    }
  }
}
