package com.sedmelluq.lavaplayer.core.info.property;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.METADATA_CORE;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.METADATA_EXTENDED;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.PLAYBACK_CORE;

public enum  AudioTrackCoreProperty {
  SOURCE(1, "source", PLAYBACK_CORE.mask | METADATA_EXTENDED.mask),
  IDENTIFIER(2, "identifier", PLAYBACK_CORE.mask),
  IS_STREAM(3, "isStream", PLAYBACK_CORE.mask | METADATA_CORE.mask),
  TITLE(4, "title", METADATA_CORE.mask),
  AUTHOR(5, "author", METADATA_CORE.mask),
  LENGTH(6, "length", METADATA_CORE.mask),
  URI(7, "uri", METADATA_EXTENDED.mask),
  ARTWORK(8, "artworkUrl", METADATA_EXTENDED.mask);

  public final int index;
  public final String name;
  public final int defaultFlags;

  AudioTrackCoreProperty(int index, String name, int defaultFlags) {
    this.index = index;
    this.name = name;
    this.defaultFlags = defaultFlags;
  }
}
