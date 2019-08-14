package com.sedmelluq.discord.lavaplayer.container.flac;

import java.util.Map;

/**
 * All relevant information about a FLAC track from its metadata.
 */
public class FlacTrackInfo {
  /**
   * FLAC stream information.
   */
  public final FlacStreamInfo stream;
  /**
   * An array of seek points.
   */
  public final FlacSeekPoint[] seekPoints;
  /**
   * The actual number of seek points that are not placeholders. The end of the array may contain empty seek points,
   * which is why this value should be used to determine how far into the array to look.
   */
  public final int seekPointCount;
  /**
   * The map of tag values from comment metadata block.
   */
  public final Map<String, String> tags;
  /**
   * The position in the stream where the first frame starts.
   */
  public final long firstFramePosition;
  /**
   * The duration of the track in milliseconds
   */
  public final long duration;

  /**
   * @param stream FLAC stream information.
   * @param seekPoints An array of seek points.
   * @param seekPointCount The actual number of seek points that are not placeholders.
   * @param tags The map of tag values from comment metadata block.
   * @param firstFramePosition The position in the stream where the first frame starts.
   */
  public FlacTrackInfo(FlacStreamInfo stream, FlacSeekPoint[] seekPoints, int seekPointCount, Map<String, String> tags,
                       long firstFramePosition) {

    this.stream = stream;
    this.seekPoints = seekPoints;
    this.seekPointCount = seekPointCount;
    this.tags = tags;
    this.firstFramePosition = firstFramePosition;
    this.duration = stream.sampleCount * 1000L / stream.sampleRate;
  }
}
