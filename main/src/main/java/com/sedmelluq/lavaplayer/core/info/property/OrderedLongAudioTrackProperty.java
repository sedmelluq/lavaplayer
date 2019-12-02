package com.sedmelluq.lavaplayer.core.info.property;

public class OrderedLongAudioTrackProperty extends LongAudioTrackProperty {
  private final int priority;

  public OrderedLongAudioTrackProperty(String name, int flags, long value, int priority) {
    super(name, flags, value);
    this.priority = priority;
  }

  @Override
  public int getPriority() {
    return priority;
  }
}
