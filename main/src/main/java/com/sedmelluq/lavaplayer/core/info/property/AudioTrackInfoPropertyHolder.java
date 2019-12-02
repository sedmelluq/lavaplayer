package com.sedmelluq.lavaplayer.core.info.property;

public interface AudioTrackInfoPropertyHolder {
  String getStringProperty(String name);

  long getLongProperty(String name);

  AudioTrackProperty getProperty(String name);

  Iterable<AudioTrackProperty> getProperties();
}
