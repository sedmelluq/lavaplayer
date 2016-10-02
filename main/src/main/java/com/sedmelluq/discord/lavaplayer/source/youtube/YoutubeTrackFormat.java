package com.sedmelluq.discord.lavaplayer.source.youtube;

import org.apache.http.entity.ContentType;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Describes an available media format for a track
 */
public class YoutubeTrackFormat {
  private final YoutubeFormatInfo info;
  private final ContentType type;
  private final long bitrate;
  private final long contentLength;
  private final String url;
  private final String signature;

  /**
   * @param type Mime type of the format
   * @param bitrate Bitrate of the format
   * @param contentLength Length in bytes of the media
   * @param url Base URL for the playback of this format
   * @param signature Cipher signature for this format
   */
  public YoutubeTrackFormat(ContentType type, long bitrate, long contentLength, String url, String signature) {
    this.info = YoutubeFormatInfo.get(type);
    this.type = type;
    this.bitrate = bitrate;
    this.contentLength = contentLength;
    this.url = url;
    this.signature = signature;
  }

  /**
   * @return Format container and codec info
   */
  public YoutubeFormatInfo getInfo() {
    return info;
  }

  /**
   * @return Mime type of the format
   */
  public ContentType getType() {
    return type;
  }

  /**
   * @return Bitrate of the format
   */
  public long getBitrate() {
    return bitrate;
  }

  /**
   * @return Base URL for the playback of this format
   */
  public URI getUrl() {
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return Length in bytes of the media
   */
  public long getContentLength() {
    return contentLength;
  }

  /**
   * @return Cipher signature for this format
   */
  public String getSignature() {
    return signature;
  }
}
