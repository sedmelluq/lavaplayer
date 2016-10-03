package com.sedmelluq.discord.lavaplayer.track.playback;

/**
 * A single OPUS audio frame with 20ms length.
 */
public class AudioFrame {
  /**
   * An AudioFrame instance which marks the end of an audio track, the time code or buffer from it should not be tried
   * to access.
   */
  public static final AudioFrame TERMINATOR = new AudioFrame(0, null, 0);

  /**
   * Timecode of this frame in milliseconds.
   */
  public final long timecode;

  /**
   * OPUS-encoded buffer for this frame.
   */
  public final byte[] data;

  /**
   * Volume level of the audio in this frame
   */
  public final int volume;

  /**
   * @param timecode Timecode of this frame in milliseconds.
   * @param data OPUS-encoded buffer for this frame.
   * @param volume Volume level of the audio in this frame
   */
  public AudioFrame(long timecode, byte[] data, int volume) {
    this.timecode = timecode;
    this.data = data;
    this.volume = volume;
  }

  /**
   * @return True if this is a terminator instance.
   */
  public boolean isTerminator() {
    return this == TERMINATOR;
  }
}
