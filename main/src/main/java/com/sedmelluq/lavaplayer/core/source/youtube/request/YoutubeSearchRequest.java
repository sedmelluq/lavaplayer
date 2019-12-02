package com.sedmelluq.lavaplayer.core.source.youtube.request;

import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;

public interface YoutubeSearchRequest extends AudioInfoRequest {
  String getSearchQuery();
}
