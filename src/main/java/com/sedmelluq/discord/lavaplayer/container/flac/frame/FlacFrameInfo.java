package com.sedmelluq.discord.lavaplayer.container.flac.frame;

/**
 * Information of a FLAC frame that is required for reading its subframes. Most of the fields in the frame info are not
 * actually needed, since it is an error if they differ from the ones specified in the file metadata.
 */
public class FlacFrameInfo {
  /**
   * Number of samples in each subframe of this frame.
   */
  public final int sampleCount;

  /**
   * The way stereo channel data is related. With stereo frames, one channel can contain its original data and the other
   * just the difference from the first one, which allows for better compression for the other channel.
   */
  public final ChannelDelta channelDelta;

  /**
   * @param sampleCount Number of samples in each subframe of this frame
   * @param channelDelta Channel data delta setting
   */
  public FlacFrameInfo(int sampleCount, ChannelDelta channelDelta) {
    this.sampleCount = sampleCount;
    this.channelDelta = channelDelta;
  }

  /**
   * The relationship between stereo channels.
   */
  public enum ChannelDelta {
    NONE(-1),
    LEFT_SIDE(1),
    RIGHT_SIDE(0),
    MID_SIDE(1);

    /**
     * The index of the channel containing delta values.
     */
    public final int deltaChannel;

    ChannelDelta(int deltaChannel) {
      this.deltaChannel = deltaChannel;
    }
  }
}
