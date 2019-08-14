package com.sedmelluq.discord.lavaplayer.track.playback;

/**
 * A frame buffer. Stores the specified duration worth of frames in the internal buffer.
 * Consumes frames in a blocking manner and provides frames in a non-blocking manner.
 */
public interface AudioFrameBuffer extends AudioFrameProvider, AudioFrameConsumer {

  /**
   * @return Number of frames that can be added to the buffer without blocking.
   */
  int getRemainingCapacity();

  /**
   * @return Total number of frames that the buffer can hold.
   */
  int getFullCapacity();

  /**
   * Wait until another thread has consumed a terminator frame from this buffer
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void waitForTermination() throws InterruptedException;

  /**
   * Signal that no more input is expected and if the content frames have been consumed, emit a terminator frame.
   */
  void setTerminateOnEmpty();

  /**
   * Signal that the next frame provided to the buffer will clear the frames before it. This is useful when the next
   * data is not contiguous with the current frame buffer, but the remaining frames in the buffer should be used until
   * the next data arrives to prevent a situation where the buffer cannot provide any frames for a while.
   */
  void setClearOnInsert();

  /**
   * @return Whether the next frame is set to clear the buffer.
   */
  boolean hasClearOnInsert();

  /**
   * Clear the buffer.
   */
  void clear();

  /**
   * Lock the buffer so no more incoming frames are accepted.
   */
  void lockBuffer();

  /**
   * @return True if this buffer has received any input frames.
   */
  boolean hasReceivedFrames();

  /**
   * @return The timecode of the last frame in the buffer, null if the buffer is empty or is marked to be cleared upon
   *         receiving the next frame.
   */
  Long getLastInputTimecode();
}
