package com.sedmelluq.lavaplayer.core.source.youtube.request;

import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequestBuilder;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;

public class YoutubeSearchRequestBuilder extends AudioInfoRequestBuilder<YoutubeSearchRequestBuilder> {
  protected String searchQuery;

  @Override
  protected YoutubeSearchRequestBuilder self() {
    return this;
  }

  public YoutubeSearchRequestBuilder withSearchQuery(String searchQuery) {
    this.searchQuery = searchQuery;
    return this;
  }

  @Override
  public AudioInfoRequest build() {
    return new DefaultYoutubeSearchRequest(responseHandler, allowedSources, orderChannelKey, customOptions,
        propertyFlagMask, properties, searchQuery);
  }
}
