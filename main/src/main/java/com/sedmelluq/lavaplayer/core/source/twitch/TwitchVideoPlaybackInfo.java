package com.sedmelluq.lavaplayer.core.source.twitch;

public class TwitchVideoPlaybackInfo {
  public final String videoId;
  public final String signature;
  public final String token;

  public TwitchVideoPlaybackInfo(String videoId, String signature, String token) {
    this.videoId = videoId;
    this.signature = signature;
    this.token = token;
  }
}
