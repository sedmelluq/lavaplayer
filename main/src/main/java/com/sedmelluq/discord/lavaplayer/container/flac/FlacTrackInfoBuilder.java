package com.sedmelluq.discord.lavaplayer.container.flac;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for FLAC track info.
 */
public class FlacTrackInfoBuilder {
  private final FlacStreamInfo streamInfo;
  private final Map<String, String> tags;
  private FlacSeekPoint[] seekPoints;
  private int seekPointCount;
  private long firstFramePosition;

  /**
   * @param streamInfo Stream info metadata block.
   */
  public FlacTrackInfoBuilder(FlacStreamInfo streamInfo) {
    this.streamInfo = streamInfo;
    this.tags = new HashMap<>();
  }

  /**
   * @return Stream info metadata block.
   */
  public FlacStreamInfo getStreamInfo() {
    return streamInfo;
  }

  /**
   * @param seekPoints Seek point array.
   * @param seekPointCount The number of seek points which are not placeholders.
   */
  public void setSeekPoints(FlacSeekPoint[] seekPoints, int seekPointCount) {
    this.seekPoints = seekPoints;
    this.seekPointCount = seekPointCount;
  }

  /**
   * @param key Name of the tag
   * @param value Value of the tag
   */
  public void addTag(String key, String value) {
    tags.put(key, value);
  }

  /**
   * @param firstFramePosition File position of the first frame
   */
  public void setFirstFramePosition(long firstFramePosition) {
    this.firstFramePosition = firstFramePosition;
  }

  /**
   * @return Track info object.
   */
  public FlacTrackInfo build() {
    return new FlacTrackInfo(streamInfo, seekPoints, seekPointCount, tags, firstFramePosition);
  }
}
