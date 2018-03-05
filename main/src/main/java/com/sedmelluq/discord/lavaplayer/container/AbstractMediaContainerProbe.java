package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SavedHeadSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

import static com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.getHeaderValue;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_ARTIST;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_TITLE;
import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.defaultOnNull;

/**
 * Abstract container detection probe
 */
public abstract class AbstractMediaContainerProbe implements MediaContainerProbe {

  /**
   * Attempts to extract track title from HTTP response headers.
   * It other case returns default {@link UNKNOWN_TITLE}
   *
   * @param inputStream Input stream that contains the track file
   * @return Track Title
   */
  protected String getDefaultTitle(SeekableInputStream inputStream) {
    PersistentHttpStream httpStream = getHttpStream(inputStream);
    if (httpStream != null) {
      return getHeaderValue(httpStream.getCurrentResponse(), "icy-description");
    }
    return UNKNOWN_TITLE;
  }

  /**
   * Attempts to extract track artist from HTTP response headers.
   * It other case returns default {@link UNKNOWN_ARTIST}
   *
   * @param inputStream Input stream that contains the track file
   * @return Track Artist
   */
  protected String getDefaultArtist(SeekableInputStream inputStream) {
    String title = null;
    PersistentHttpStream httpStream = getHttpStream(inputStream);
    if (httpStream != null) {
      title = getHeaderValue(httpStream.getCurrentResponse(), "icy-name");
      if (title == null) {
        title = getHeaderValue(httpStream.getCurrentResponse(), "icy-url");
      }
    }
    return defaultOnNull(title, UNKNOWN_ARTIST);
  }

  /**
   * Attempts to extract track url from HTTP response headers.

   * @param inputStream Input stream that contains the track file
   * @return Url
   */
  protected String getDefaultUrl(SeekableInputStream inputStream) {
    PersistentHttpStream httpStream = getHttpStream(inputStream);
    if (httpStream != null) {
      return getHeaderValue(httpStream.getCurrentResponse(), "icy-url");
    }
    return null;
  }

  /**
   * Returns HTTP stream if present as current class or underlaying delegate of {@link SavedHeadSeekableInputStream}
   * @param inputStream Input stream that contains the track file
   * @return An {@link PersistentHttpStream} stream object
   */
  private PersistentHttpStream getHttpStream(SeekableInputStream inputStream) {
    if (inputStream instanceof SavedHeadSeekableInputStream) {
      inputStream = ((SavedHeadSeekableInputStream) inputStream).getDelegate();
    }
    if (inputStream instanceof PersistentHttpStream) {
      return (PersistentHttpStream) inputStream;
    }
    return null;
  }
}
