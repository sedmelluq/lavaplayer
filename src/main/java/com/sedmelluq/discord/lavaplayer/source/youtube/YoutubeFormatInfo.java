package com.sedmelluq.discord.lavaplayer.source.youtube;

import org.apache.http.entity.ContentType;

import static com.sedmelluq.discord.lavaplayer.container.Formats.*;

/**
 * The mime type and codec info of a Youtube track format.
 */
public enum YoutubeFormatInfo {
  WEBM_OPUS(MIME_AUDIO_WEBM, CODEC_OPUS),
  WEBM_VORBIS(MIME_AUDIO_WEBM, CODEC_VORBIS),
  MP4_AAC_LC(MIME_AUDIO_MP4, CODEC_AAC_LC),
  WEBM_VIDEO_VORBIS(MIME_VIDEO_WEBM, CODEC_VORBIS),
  MP4_VIDEO_AAC_LC(MIME_VIDEO_MP4, CODEC_AAC_LC);

  /**
   * Mime type of the format
   */
  public final String mimeType;
  /**
   * Codec name of  the format
   */
  public final String codec;

  YoutubeFormatInfo(String mimeType, String codec) {
    this.mimeType = mimeType;
    this.codec = codec;
  }

  /**
   * Find a matching format info instance from a content type.
   * @param contentType The content type to use for matching against known formats
   * @return The format info entry that matches the content type
   */
  public static YoutubeFormatInfo get(ContentType contentType) {
    String mimeType = contentType.getMimeType();
    String codec = contentType.getParameter("codecs");

    // Check accurate matches
    for (YoutubeFormatInfo formatInfo : YoutubeFormatInfo.class.getEnumConstants()) {
      if (formatInfo.mimeType.equals(mimeType) && formatInfo.codec.equals(codec)) {
        return formatInfo;
      }
    }

    // Check for substring matches
    for (YoutubeFormatInfo formatInfo : YoutubeFormatInfo.class.getEnumConstants()) {
      if (formatInfo.mimeType.equals(mimeType) && codec.contains(formatInfo.codec)) {
        return formatInfo;
      }
    }

    return null;
  }
}
