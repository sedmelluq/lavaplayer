package com.sedmelluq.lavaplayer.core.info.property;

public class StringAudioTrackProperty extends AudioTrackProperty {
  private final String value;

  public StringAudioTrackProperty(String name, int flags, String value) {
    super(name, flags);
    this.value = value;
  }

  @Override
  public long longValue() {
    return 0;
  }

  @Override
  public String stringValue() {
    return value;
  }
}
