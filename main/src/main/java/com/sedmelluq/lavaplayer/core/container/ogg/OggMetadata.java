package com.sedmelluq.lavaplayer.core.container.ogg;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoProvider;
import java.util.Collections;
import java.util.Map;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;

/**
 * Audio track info provider based on OGG metadata map.
 */
public class OggMetadata implements AudioTrackInfoProvider {
  public static final OggMetadata EMPTY = new OggMetadata(Collections.emptyMap(), Long.MAX_VALUE);

  private static final String TITLE_FIELD = "TITLE";
  private static final String ARTIST_FIELD = "ARTIST";

  private final Map<String, String> tags;
  private final Long length;

  /**
   * @param tags Map of OGG metadata with OGG-specific keys.
   */
  public OggMetadata(Map<String, String> tags, Long length) {
    this.tags = tags;
    this.length = length;
  }

  @Override
  public void provideTrackInfo(AudioTrackInfoBuilder builder) {
    builder
        .with(coreTitle(tags.get(TITLE_FIELD)))
        .with(coreAuthor(tags.get(ARTIST_FIELD)));

    if (length != null) {
      builder.with(coreLength(length));
    }
  }
}
