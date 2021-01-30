package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.container.mp3.Mp3TrackProvider;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.util.function.Supplier;

public class SoundCloudMp3SegmentDecoder implements SoundCloudSegmentDecoder {
  private final Supplier<SeekableInputStream> nextStreamProvider;

  public SoundCloudMp3SegmentDecoder(Supplier<SeekableInputStream> nextStreamProvider) {
    this.nextStreamProvider = nextStreamProvider;
  }

  @Override
  public void prepareStream(boolean beginning) {
    // Nothing to do.
  }

  @Override
  public void resetStream() {
    // Nothing to do.
  }

  @Override
  public void playStream(
      AudioPlaybackContext context,
      long startPosition,
      long desiredPosition
  ) throws InterruptedException, IOException {
    try (SeekableInputStream stream = nextStreamProvider.get()) {
      try (Mp3TrackProvider trackProvider = new Mp3TrackProvider(context, stream)) {
        trackProvider.parseHeaders();
        trackProvider.provideFrames();
      }
    }
  }

  @Override
  public void close() {
    // Nothing to do.
  }
}
