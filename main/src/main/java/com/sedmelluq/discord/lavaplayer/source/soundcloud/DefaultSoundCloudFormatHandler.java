package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import java.util.List;

public class DefaultSoundCloudFormatHandler implements SoundCloudFormatHandler {
  @Override
  public SoundCloudTrackFormat chooseBestFormat(List<SoundCloudTrackFormat> formats) {
    for (SoundCloudTrackFormat format : formats) {
      if ("hls".equals(format.getProtocol()) && format.getMimeType().contains("audio/ogg")) {
        return format;
      }
    }

    for (SoundCloudTrackFormat format : formats) {
      if ("progressive".equals(format.getProtocol()) && format.getMimeType().contains("audio/mpeg")) {
        return format;
      }
    }

    throw new RuntimeException("Did not detect any supported formats");
  }

  @Override
  public String buildFormatIdentifier(SoundCloudTrackFormat format) {
    if ("hls".equals(format.getProtocol())) {
      return "O:" + format.getLookupUrl();
    } else if ("progressive".equals(format.getProtocol())) {
      return "M:" + format.getLookupUrl();
    } else {
      return "X:" + format.getLookupUrl();
    }
  }

  @Override
  public String getOpusLookupUrl(String identifier) {
    if (identifier.startsWith("O:")) {
      return identifier.substring(2);
    }

    return null;
  }

  @Override
  public String getMp3LookupUrl(String identifier) {
    if (identifier.startsWith("M:")) {
      return identifier.substring(2);
    }

    return null;
  }
}
