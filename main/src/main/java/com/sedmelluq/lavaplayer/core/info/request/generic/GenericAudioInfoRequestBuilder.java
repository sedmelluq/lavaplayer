package com.sedmelluq.lavaplayer.core.info.request.generic;

import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequestBuilder;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;

public class GenericAudioInfoRequestBuilder extends AudioInfoRequestBuilder<GenericAudioInfoRequestBuilder> {
  protected String hint;

  @Override
  protected GenericAudioInfoRequestBuilder self() {
    return this;
  }

  @Override
  public AudioInfoRequest build() {
    return new DefaultGenericAudioInfoRequest(responseHandler, allowedSources, orderChannelKey, customOptions,
        propertyFlagMask, properties, hint);
  }

  public GenericAudioInfoRequestBuilder withHint(String hint) {
    this.hint = hint;
    return self();
  }
}
