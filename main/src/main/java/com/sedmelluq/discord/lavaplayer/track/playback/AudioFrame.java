package com.sedmelluq.discord.lavaplayer.track.playback;

/**
 * A single OPUS audio frame with 20ms length.
 */
public class AudioFrame {
  /**
   * An AudioFrame instance which marks the end of an audio track, the time code or buffer from it should not be tried
   * to access.
   */
  public static final AudioFrame TERMINATOR = new AudioFrame(0, null);

  /**
   * Timecode of this frame in milliseconds.
   */
  public final long timecode;

  /**
   * OPUS-encoded buffer for this frame.
   */
  public final byte[] data;

  /**
   * @param timecode Timecode of this frame in milliseconds.
   * @param data OPUS-encoded buffer for this frame.
   */
  public AudioFrame(long timecode, byte[] data) {
    this.timecode = timecode;
    this.data = data;
  }

  /**
   * @return True if this is a terminator instance.
   */
  public boolean isTerminator() {
    return this == TERMINATOR;
  }
}
