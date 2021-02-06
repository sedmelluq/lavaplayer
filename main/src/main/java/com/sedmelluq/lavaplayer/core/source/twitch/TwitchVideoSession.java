package com.sedmelluq.lavaplayer.core.source.twitch;

public class TwitchVideoSession {
  public final String clientId;
  public final String deviceId;

  public TwitchVideoSession(String clientId, String deviceId) {
    this.clientId = clientId;
    this.deviceId = deviceId;
  }
}
