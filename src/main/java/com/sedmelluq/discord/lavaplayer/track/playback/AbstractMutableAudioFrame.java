package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

/**
 * Base class for mutable audio frames.
 */
public abstract class AbstractMutableAudioFrame implements AudioFrame {
  private long timecode;
  private int volume;
  private AudioDataFormat format;
  private boolean terminator;

  @Override
  public long getTimecode() {
    return timecode;
  }

  public void setTimecode(long timecode) {
    this.timecode = timecode;
  }

  @Override
  public int getVolume() {
    return volume;
  }

  public void setVolume(int volume) {
    this.volume = volume;
  }

  @Override
  public AudioDataFormat getFormat() {
    return format;
  }

  public void setFormat(AudioDataFormat format) {
    this.format = format;
  }

  @Override
  public boolean isTerminator() {
    return terminator;
  }

  public void setTerminator(boolean terminator) {
    this.terminator = terminator;
  }

  /**
   * @return An immutable instance created from this mutable audio frame. In an ideal flow, this should never be called.
   */
  public ImmutableAudioFrame freeze() {
    return new ImmutableAudioFrame(timecode, getData(), volume, format);
  }
}
