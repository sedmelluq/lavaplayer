package com.sedmelluq.lavaplayer.core.source.soundcloud;

public interface SoundCloudTrackFormat {
  String getTrackId();

  String getProtocol();

  String getMimeType();

  String getLookupUrl();
}
