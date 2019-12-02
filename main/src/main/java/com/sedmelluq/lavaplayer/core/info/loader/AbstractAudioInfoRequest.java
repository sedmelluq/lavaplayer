package com.sedmelluq.lavaplayer.core.info.loader;

import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractAudioInfoRequest implements AudioInfoRequest {
  protected final AudioInfoResponseHandler resultConsumer;
  protected final Set<Class<? extends AudioSource>> allowedSources;
  protected final Object orderChannelKey;
  protected final Map<String, Object> customOptions;
  protected final int propertyFlagMask;
  protected final List<AudioTrackProperty> properties;

  protected AbstractAudioInfoRequest(
      AudioInfoResponseHandler resultConsumer,
      Set<Class<? extends AudioSource>> allowedSources,
      Object orderChannelKey,
      Map<String, Object> customOptions,
      int propertyFlagMask, List<AudioTrackProperty> properties
  ) {
    this.resultConsumer = resultConsumer;
    this.allowedSources = allowedSources;
    this.orderChannelKey = orderChannelKey;
    this.customOptions = customOptions != null ? customOptions : Collections.emptyMap();
    this.propertyFlagMask = propertyFlagMask;
    this.properties = properties;
  }

  @Override
  public AudioInfoResponseHandler getResponseHandler() {
    return resultConsumer;
  }

  @Override
  public boolean isSourceAllowed(AudioSource source) {
    if (allowedSources == null) {
      return true;
    } else {
      return allowedSources.contains(source.getClass());
    }
  }

  @Override
  public Object getOrderChannelKey() {
    return orderChannelKey;
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

  @Override
  public Map<String, Object> getCustomOptions() {
    return customOptions;
  }

  @Override
  public int getPropertyFlagMask() {
    return propertyFlagMask;
  }

  @Override
  public List<AudioTrackProperty> getProperties() {
    return properties;
  }
}
