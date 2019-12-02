package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.info.track.ExtendedAudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;

public class YoutubeAudioTrackInfo extends ExtendedAudioTrackInfo {
  public YoutubeAudioTrackInfo(AudioTrackInfo delegate) {
    super(delegate);
  }

  public String getVideoId() {
    return getIdentifier();
  }

  @Override
  public String getUri() {
    String explicitUri = super.getArtworkUrl();

    if (explicitUri == null) {
      return "https://www.youtube.com/watch?v=" + getVideoId();
    } else {
      return explicitUri;
    }
  }

  @Override
  public String getArtworkUrl() {
    String explicitUrl = super.getArtworkUrl();

    if (explicitUrl == null) {
      return "https://img.youtube.com/vi/" + getIdentifier() + "/0.jpg";
    } else {
      return explicitUrl;
    }
  }
}
