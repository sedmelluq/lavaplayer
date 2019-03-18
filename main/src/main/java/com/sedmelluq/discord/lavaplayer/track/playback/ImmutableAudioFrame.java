package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

/**
 * A single audio frame.
 */
public class ImmutableAudioFrame implements AudioFrame {
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
  public ImmutableAudioFrame(long timecode, byte[] data, int volume, AudioDataFormat format) {
    this.timecode = timecode;
    this.data = data;
    this.volume = volume;
    this.format = format;
  }

  @Override
  public long getTimecode() {
    return timecode;
  }

  @Override
  public int getVolume() {
    return volume;
  }

  @Override
  public int getDataLength() {
    return data.length;
  }

  @Override
  public byte[] getData() {
    return data;
  }

  @Override
  public void getData(byte[] buffer, int offset) {
    System.arraycopy(data, 0, buffer, offset, data.length);
  }

  @Override
  public AudioDataFormat getFormat() {
    return format;
  }

  @Override
  public boolean isTerminator() {
    return false;
  }
}