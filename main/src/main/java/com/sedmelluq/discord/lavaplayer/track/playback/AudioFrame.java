package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

/**
 * A single audio frame.
 */
public class AudioFrame {
  /**
   * An AudioFrame instance which marks the end of an audio track, the time code or buffer from it should not be tried
   * to access.
   */
  public static final AudioFrame TERMINATOR = new AudioFrame(0, null, 0, null);

  /**
   * Timecode of this frame in milliseconds.
   */
  public final long timecode;

  /**
   * Buffer for this frame, in the format specified in the format field.
   */
  public final byte[] data;

  /**
   * Volume level of the audio in this frame. Internally when this value is 0, the data may actually contain a
   * non-silent frame. This is to allow frames with 0 volume to be modified later. These frames should still be
   * handled as silent frames.
   */
  public final int volume;

  /**
   * Specifies the format of audio in the data buffer.
   */
  public final AudioDataFormat format;

  /**
   * @param timecode Timecode of this frame in milliseconds.
   * @param data Buffer for this frame, in the format specified in the format field.
   * @param volume Volume level of the audio in this frame.
   * @param format Specifies the format of audio in the data buffer.
   */
  public AudioFrame(long timecode, byte[] data, int volume, AudioDataFormat format) {
    this.timecode = timecode;
    this.data = data;
    this.volume = volume;
    this.format = format;
  }

  /**
   * @return True if this is a terminator instance.
   */
  public boolean isTerminator() {
    return this == TERMINATOR;
  }
}
