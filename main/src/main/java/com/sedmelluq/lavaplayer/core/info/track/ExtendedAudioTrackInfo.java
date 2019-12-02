package com.sedmelluq.lavaplayer.core.info.track;

import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import java.util.Collections;

public abstract class ExtendedAudioTrackInfo extends CoreAudioTrackInfo {
  private final AudioTrackInfo delegate;

  public ExtendedAudioTrackInfo(AudioTrackInfo delegate) {
    super(Collections.emptyMap());
    this.delegate = delegate;
  }

  @Override
  public AudioTrackProperty getProperty(String name) {
    return delegate.getProperty(name);
  }

  @Override
  public Iterable<AudioTrackProperty> getProperties() {
    return delegate.getProperties();
  }
}
