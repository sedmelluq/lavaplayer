package com.sedmelluq.lavaplayer.core.info.property;

public class LongAudioTrackProperty extends AudioTrackProperty {
  private final long value;

  public LongAudioTrackProperty(String name, int flags, long value) {
    super(name, flags);
    this.value = value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public String stringValue() {
    return String.valueOf(value);
  }
}
