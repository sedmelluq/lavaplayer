package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

/**
 * Audio frame where {@link #isTerminator()} is <code>true</code>.
 */
public class TerminatorAudioFrame implements AudioFrame {
  public static final TerminatorAudioFrame INSTANCE = new TerminatorAudioFrame();

  @Override
  public long getTimecode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getVolume() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getDataLength() {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getData() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void getData(byte[] buffer, int offset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AudioDataFormat getFormat() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isTerminator() {
    return true;
  }
}
