package com.sedmelluq.discord.lavaplayer.track.info;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

/**
 * Builder for {@link AudioTrackInfo}.
 */
public class AudioTrackInfoBuilder implements AudioTrackInfoProvider {
  private static final String UNKNOWN_TITLE = "Unknown title";
  private static final String UNKNOWN_ARTIST = "Unknown artist";

  private String title;
  private String author;
  private Long length;
  private String identifier;
  private String uri;
  private Boolean isStream;

  private AudioTrackInfoBuilder() {

  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getAuthor() {
    return author;
  }

  @Override
  public Long getLength() {
    return length;
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public String getUri() {
    return uri;
  }

  public AudioTrackInfoBuilder setTitle(String value) {
    title = DataFormatTools.defaultOnNull(value, title);
    return this;
  }

  public AudioTrackInfoBuilder setAuthor(String value) {
    author = DataFormatTools.defaultOnNull(value, author);
    return this;
  }

  public AudioTrackInfoBuilder setLength(Long value) {
    length = DataFormatTools.defaultOnNull(value, length);
    return this;
  }

  public AudioTrackInfoBuilder setIdentifier(String value) {
    identifier = DataFormatTools.defaultOnNull(value, identifier);
    return this;
  }

  public AudioTrackInfoBuilder setUri(String value) {
    uri = DataFormatTools.defaultOnNull(value, uri);
    return this;
  }

  public AudioTrackInfoBuilder setIsStream(Boolean stream) {
    isStream = stream;
    return this;
  }

  /**
   * @param provider The track info provider to apply to the builder.
   * @return this
   */
  public AudioTrackInfoBuilder apply(AudioTrackInfoProvider provider) {
    if (provider == null) {
      return this;
    }

    return setTitle(provider.getTitle())
        .setAuthor(provider.getAuthor())
        .setLength(provider.getLength())
        .setIdentifier(provider.getIdentifier())
        .setUri(provider.getIdentifier());
  }

  /**
   * @return Audio track info instance.
   */
  public AudioTrackInfo build() {
    long finalLength = DataFormatTools.defaultOnNull(length, Long.MAX_VALUE);

    return new AudioTrackInfo(
        title,
        author,
        finalLength,
        identifier,
        DataFormatTools.defaultOnNull(isStream, finalLength == Long.MAX_VALUE),
        uri
    );
  }

  /**
   * Creates an instance of an audio track builder based on an audio reference and a stream.
   *
   * @param reference Audio reference to use as the starting point for the builder.
   * @param stream Stream to get additional data from.
   * @return An instance of the builder with the reference and track info providers from the stream preapplied.
   */
  public static AudioTrackInfoBuilder create(AudioReference reference, SeekableInputStream stream) {
    AudioTrackInfoBuilder builder = new AudioTrackInfoBuilder()
        .setAuthor(UNKNOWN_ARTIST)
        .setTitle(UNKNOWN_TITLE)
        .setLength(Long.MAX_VALUE);

    builder.apply(reference);

    for (AudioTrackInfoProvider provider : stream.getTrackInfoProviders()) {
      builder.apply(provider);
    }

    return builder;
  }

  /**
   * @return Empty instance of audio track builder.
   */
  public static AudioTrackInfoBuilder empty() {
    return new AudioTrackInfoBuilder();
  }
}
