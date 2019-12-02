package com.sedmelluq.lavaplayer.core.info.property;

import java.util.Map;

public class AbstractAudioTrackInfoPropertyHolder implements AudioTrackInfoPropertyHolder {
  private final Map<String, AudioTrackProperty> properties;

  public AbstractAudioTrackInfoPropertyHolder(Map<String, AudioTrackProperty> properties) {
    this.properties = properties;
  }

  @Override
  public String getStringProperty(String name) {
    AudioTrackProperty property = getProperty(name);
    return property != null ? property.stringValue() : null;
  }

  @Override
  public long getLongProperty(String name) {
    AudioTrackProperty property = getProperty(name);
    return property != null ? property.longValue() : 0;
  }

  @Override
  public AudioTrackProperty getProperty(String name) {
    return properties.get(name);
  }

  @Override
  public Iterable<AudioTrackProperty> getProperties() {
    return properties.values();
  }
}
