package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;

import java.util.Collections;
import java.util.Map;

/**
 * Audio track info provider based on OGG metadata map.
 */
public class OggMetadata implements AudioTrackInfoProvider {
  public static final OggMetadata EMPTY = new OggMetadata(Collections.emptyMap());

  private static final String TITLE_FIELD = "TITLE";
  private static final String ARTIST_FIELD = "ARTIST";

  private final Map<String, String> tags;

  /**
   * @param tags Map of OGG metadata with OGG-specific keys.
   */
  public OggMetadata(Map<String, String> tags) {
    this.tags = tags;
  }

  @Override
  public String getTitle() {
    return tags.get(TITLE_FIELD);
  }

  @Override
  public String getAuthor() {
    return tags.get(ARTIST_FIELD);
  }

  @Override
  public Long getLength() {
    return null;
  }

  @Override
  public String getIdentifier() {
    return null;
  }

  @Override
  public String getUri() {
    return null;
  }
}
