package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import java.util.List;

public interface SoundCloudFormatHandler {
  SoundCloudTrackFormat chooseBestFormat(List<SoundCloudTrackFormat> formats);

  String buildFormatIdentifier(SoundCloudTrackFormat format);

  String getOpusLookupUrl(String identifier);

  String getMp3LookupUrl(String identifier);
}
