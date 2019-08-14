package com.sedmelluq.discord.lavaplayer.container.mpeg;

import java.nio.channels.ReadableByteChannel;

/**
 * No-op MP4 track consumer, for probing purposes.
 */
public class MpegNoopTrackConsumer implements MpegTrackConsumer {
  private final MpegTrackInfo trackInfo;

  /**
   * @param trackInfo Track info.
   */
  public MpegNoopTrackConsumer(MpegTrackInfo trackInfo) {
    this.trackInfo = trackInfo;
  }

  @Override
  public MpegTrackInfo getTrack() {
    return trackInfo;
  }

  @Override
  public void initialise() {
    // Nothing to do
  }

  @Override
  public void seekPerformed(long requestedTimecode, long providedTimecode) {
    // Nothing to do
  }

  @Override
  public void flush() throws InterruptedException {
    // Nothing to do
  }

  @Override
  public void consume(ReadableByteChannel channel, int length) throws InterruptedException {
    // Nothing to do
  }

  @Override
  public void close() {
    // Nothing to do
  }
}
