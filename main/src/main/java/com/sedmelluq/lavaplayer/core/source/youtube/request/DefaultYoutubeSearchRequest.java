package com.sedmelluq.lavaplayer.core.source.youtube.request;

import com.sedmelluq.lavaplayer.core.info.loader.AbstractAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoResponseHandler;
import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultYoutubeSearchRequest extends AbstractAudioInfoRequest implements YoutubeSearchRequest {
  protected final String searchQuery;
  protected final String name;

  public DefaultYoutubeSearchRequest(
      AudioInfoResponseHandler resultConsumer,
      Set<Class<? extends AudioSource>> allowedSources,
      Object orderChannelKey,
      Map<String, Object> customOptions,
      int propertyFlagMask,
      List<AudioTrackProperty> properties,
      String searchQuery
  ) {
    super(resultConsumer, allowedSources, orderChannelKey, customOptions, propertyFlagMask, properties);

    this.searchQuery = searchQuery;
    name = "YoutubeSearch<" + searchQuery + ">";
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String getSearchQuery() {
    return searchQuery;
  }
}
