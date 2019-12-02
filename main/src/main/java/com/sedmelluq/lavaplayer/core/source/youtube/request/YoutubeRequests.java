package com.sedmelluq.lavaplayer.core.source.youtube.request;

import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoResponseHandler;
import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import java.util.Collections;

public class YoutubeRequests {
  public static YoutubeSearchRequest search(String searchQuery, AudioInfoResponseHandler consumer) {
    return orderedSearch(searchQuery, consumer, null);
  }

  public static YoutubeSearchRequest orderedSearch(String searchQuery, AudioInfoResponseHandler consumer, Object orderKey) {
    return new DefaultYoutubeSearchRequest(consumer, null, orderKey, null,
        AudioTrackProperty.Flag.fullMask(), Collections.emptyList(), searchQuery);
  }

  public static YoutubeSearchRequestBuilder searchBuilder(String searchQuery) {
    return new YoutubeSearchRequestBuilder()
        .withSearchQuery(searchQuery);
  }
}
