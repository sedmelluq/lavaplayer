package com.sedmelluq.lavaplayer.core.info.request.generic;

import com.sedmelluq.lavaplayer.core.info.loader.AbstractAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoResponseHandler;
import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultGenericAudioInfoRequest extends AbstractAudioInfoRequest implements GenericAudioInfoRequest {
  private final String hint;
  private final String name;

  public DefaultGenericAudioInfoRequest(
      AudioInfoResponseHandler resultConsumer,
      Set<Class<? extends AudioSource>> allowedSources,
      Object orderChannelKey,
      Map<String, Object> customOptions,
      int propertyFlagMask,
      List<AudioTrackProperty> properties,
      String hint
  ) {
    super(resultConsumer, allowedSources, orderChannelKey, customOptions, propertyFlagMask, properties);
    this.hint = hint;
    this.name = "Generic<" + hint + ">";
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String getHint() {
    return hint;
  }
}
