package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

/**
 * Represents an audio frame.
 */
public interface AudioFrame {
  /**
   * @return Absolute timecode of the frame in milliseconds.
   */
  long getTimecode();

  /**
   * @return Volume of the current frame.
   */
  int getVolume();

  /**
   * @return Length of the data of this frame.
   */
  int getDataLength();

  /**
   * @return Byte array with the frame data.
   */
  byte[] getData();

  /**
   * Before calling this method, the caller should verify that the data fits in the buffer using
   * {@link #getDataLength()}.
   *
   * @param buffer Buffer to write the frame data to.
   * @param offset Offset in the buffer to start writing at.
   */
  void getData(byte[] buffer, int offset);

  /**
   * @return The data format of this buffer.
   */
  AudioDataFormat getFormat();

  /**
   * @return Whether this frame is a terminator. This is an internal concept of the player and should never be
   *         <code>true</code> in any frames received by the user.
   */
  boolean isTerminator();
}



