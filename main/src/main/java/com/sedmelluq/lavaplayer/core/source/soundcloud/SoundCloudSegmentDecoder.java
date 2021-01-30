package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.util.function.Supplier;

public interface SoundCloudSegmentDecoder extends AutoCloseable {
  void prepareStream(boolean beginning) throws IOException;

  void resetStream() throws IOException;

  void playStream(
      AudioPlaybackContext context,
      long startPosition,
      long desiredPosition
  ) throws InterruptedException, IOException;

  interface Factory {
    SoundCloudSegmentDecoder create(Supplier<SeekableInputStream> nextStreamProvider);
  }
}
