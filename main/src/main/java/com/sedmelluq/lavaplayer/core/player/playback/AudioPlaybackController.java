package com.sedmelluq.lavaplayer.core.player.playback;

public interface AudioPlaybackController {
  AudioPlaybackContext getContext();

  default void executeProcessingLoop(ReadExecutor readExecutor, SeekExecutor seekExecutor) {
    executeProcessingLoop(readExecutor, seekExecutor, true);
  }

  void executeProcessingLoop(ReadExecutor readExecutor, SeekExecutor seekExecutor, boolean waitOnEnd);

  void updateDuration(long duration);

  /**
   * Read executor, see method description
   */
  interface ReadExecutor {
    /**
     * Reads until interrupted or EOF.
     *
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    void performRead() throws Exception;
  }

  /**
   * Seek executor, see method description
   */
  interface SeekExecutor {
    /**
     * Perform a seek to the specified position
     *
     * @param position Position in milliseconds
     */
    void performSeek(long position) throws Exception;
  }
}
