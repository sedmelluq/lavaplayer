package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import java.util.List;

public class DefaultSoundCloudFormatHandler implements SoundCloudFormatHandler {
  private static final FormatType[] TYPES = FormatType.values();

  @Override
  public SoundCloudTrackFormat chooseBestFormat(List<SoundCloudTrackFormat> formats) {
    for (FormatType type : TYPES) {
      for (SoundCloudTrackFormat format : formats) {
        if (type.matches(format)) {
          return format;
        }
      }
    }

    throw new RuntimeException("Did not detect any supported formats");
  }

  @Override
  public String buildFormatIdentifier(SoundCloudTrackFormat format) {
    for (FormatType type : TYPES) {
      if (type.matches(format)) {
        return type.prefix + format.getLookupUrl();
      }
    }

    return "X:" + format.getLookupUrl();
  }

  @Override
  public SoundCloudM3uInfo getM3uInfo(String identifier) {
    if (identifier.startsWith(FormatType.TYPE_M3U_OPUS.prefix)) {
      return new SoundCloudM3uInfo(identifier.substring(2), SoundCloudOpusSegmentDecoder::new);
    } else if (identifier.startsWith(FormatType.TYPE_M3U_MP3.prefix)) {
      return new SoundCloudM3uInfo(identifier.substring(2), SoundCloudMp3SegmentDecoder::new);
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

  private static SoundCloudTrackFormat findFormat(List<SoundCloudTrackFormat> formats, FormatType type) {
    for (SoundCloudTrackFormat format : formats) {
      if (type.matches(format)) {
        return format;
      }
    }

    return null;
  }

  private enum FormatType {
    TYPE_M3U_OPUS("hls", "audio/ogg", "O:"),
    TYPE_M3U_MP3("hls", "audio/mpeg", "U:"),
    TYPE_SIMPLE_MP3("progressive", "audio/mpeg", "M:");

    public final String protocol;
    public final String mimeType;
    public final String prefix;

    FormatType(String protocol, String mimeType, String prefix) {
      this.protocol = protocol;
      this.mimeType = mimeType;
      this.prefix = prefix;
    }

    public boolean matches(SoundCloudTrackFormat format) {
      return protocol.equals(format.getProtocol()) && format.getMimeType().contains(mimeType);
    }
  }
}
