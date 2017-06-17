package com.sedmelluq.discord.lavaplayer.container.matroska.format;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface MatroskaBlock {
  /**
   * @return The timecode of this block relative to its cluster
   */
  int getTimecode();

  /**
   * @return The track number which this block is for
   */
  int getTrackNumber();

  /**
   * @return Whether this block is a keyframe
   */
  boolean isKeyFrame();

  /**
   * @return The number of frames in this block
   */
  int getFrameCount();

  /**
   * The reader must already be positioned at the frame that is to be read next.
   *
   * @param index The index of the frame to get the buffer for.
   * @return A buffer where the range between position and limit contains the data for the specific frame. The contents
   *         of this buffer are only valid until the next call to this method.
   */
  ByteBuffer getNextFrameBuffer(MatroskaFileReader reader, int index) throws IOException;
}
