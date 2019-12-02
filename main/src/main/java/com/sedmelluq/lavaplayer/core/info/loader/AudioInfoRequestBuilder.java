package com.sedmelluq.lavaplayer.core.info.loader;

import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AudioInfoRequestBuilder<T extends AudioInfoRequestBuilder<T>> {
  protected AudioInfoResponseHandler responseHandler;
  protected Set<Class<? extends AudioSource>> allowedSources;
  protected Object orderChannelKey;
  protected Map<String, Object> customOptions;
  protected int propertyFlagMask;
  protected List<AudioTrackProperty> properties;
  protected boolean inheritedCustomOptions;
  protected boolean inheritedProperties;

  protected abstract T self();

  public T withResponseHandler(AudioInfoResponseHandler consumer) {
    this.responseHandler = consumer;
    return self();
  }

  public T withAllowedSources(Set<Class<? extends AudioSource>> allowedSources) {
    this.allowedSources = allowedSources;
    return self();
  }

  public T withOrderChannelKey(Object orderChannelKey) {
    this.orderChannelKey = orderChannelKey;
    return self();
  }

  public T withCustomOption(String name, Object value) {
    if (customOptions == null) {
      customOptions = new HashMap<>();
    } else if (inheritedCustomOptions) {
      customOptions = new HashMap<>(customOptions);
    }

    inheritedCustomOptions = false;
    customOptions.put(name, value);
    return self();
  }

  public T withPropertyFlagMask(int propertyFlagMask) {
    this.propertyFlagMask = propertyFlagMask;
    return self();
  }

  public T withInheritedFields(AudioInfoRequest request) {
    withPropertyFlagMask(request.getPropertyFlagMask());

    properties = request.getProperties();
    inheritedProperties = true;
    customOptions = request.getCustomOptions();
    inheritedCustomOptions = true;

    return self();
  }

  public T withProperties(Iterable<AudioTrackProperty> properties) {
    return self();
  }

  public T withProperty(AudioTrackProperty property) {
    if (property != null) {
      if (properties == null) {
        properties = new ArrayList<>(4);
      } else if (inheritedProperties) {
        properties = new ArrayList<>(properties);
      }

      inheritedProperties = false;
      properties.add(property);
    }

    return self();
  }

  public abstract AudioInfoRequest build();
}
