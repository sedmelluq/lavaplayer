package com.sedmelluq.discord.lavaplayer.track.playback;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A provider for audio frames
 */
public interface AudioFrameProvider {
  /**
   * @return Provided frame, or null if none available
   */
  AudioFrame provide();

  /**
   * @param timeout Specifies the maximum time to wait for data. Pass 0 for non-blocking mode.
   * @param unit Specifies the time unit of the maximum wait time.
   * @return Provided frame. In case wait time is above zero, null indicates that no data is not available at the
   *         current moment, otherwise null means the end of the track.
   * @throws TimeoutException When wait time is above zero, but no track info is found in that time.
   * @throws InterruptedException When wait is interrupted.
   */
  AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException;
}
