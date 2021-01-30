package com.sedmelluq.lavaplayer.core.source.soundcloud;

import java.util.List;

public interface SoundCloudFormatHandler {
  SoundCloudTrackFormat chooseBestFormat(List<SoundCloudTrackFormat> formats);

  String buildFormatIdentifier(SoundCloudTrackFormat format);

  SoundCloudM3uInfo getM3uInfo(String identifier);

  String getMp3LookupUrl(String identifier);
}
