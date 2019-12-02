package com.sedmelluq.lavaplayer.core.info.property;

public class OrderedStringAudioTrackProperty extends StringAudioTrackProperty {
  private final int priority;

  public OrderedStringAudioTrackProperty(String name, int flags, String value, int priority) {
    super(name, flags, value);
    this.priority = priority;
  }

  @Override
  public int getPriority() {
    return priority;
  }
}
