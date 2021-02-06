package com.sedmelluq.lavaplayer.core.source.twitch;

import com.sedmelluq.lavaplayer.core.container.adts.AdtsStreamProvider;
import com.sedmelluq.lavaplayer.core.container.mpegts.MpegTsElementaryInputStream;
import com.sedmelluq.lavaplayer.core.container.mpegts.PesPacketInputStream;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import com.sedmelluq.lavaplayer.core.source.soundcloud.SoundCloudSegmentDecoder;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.util.function.Supplier;

import static com.sedmelluq.lavaplayer.core.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;

public class TwitchMpegSegmentDecoder implements SoundCloudSegmentDecoder {
  private final Supplier<SeekableInputStream> nextStreamProvider;

  public TwitchMpegSegmentDecoder(Supplier<SeekableInputStream> nextStreamProvider) {
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
  ) throws InterruptedException {
    try (SeekableInputStream inputStream = nextStreamProvider.get()) {
      MpegTsElementaryInputStream elementaryInputStream = new MpegTsElementaryInputStream(inputStream, ADTS_ELEMENTARY_STREAM);
      PesPacketInputStream packetInputStream = new PesPacketInputStream(elementaryInputStream);

      try (AdtsStreamProvider provider = new AdtsStreamProvider(packetInputStream, context)) {
        provider.setInitialSeek(startPosition, desiredPosition);
        provider.provideFrames();
      }
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    resetStream();
  }
}
