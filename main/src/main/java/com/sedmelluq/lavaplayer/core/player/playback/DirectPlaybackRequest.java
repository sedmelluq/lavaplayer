package com.sedmelluq.lavaplayer.core.player.playback;

import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.player.AudioTrackRequestBuilder;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackRequest;
import com.sedmelluq.lavaplayer.core.source.AudioSource;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIdentifier;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;

public class DirectPlaybackRequest {
  private static final AudioTrackInfo DUMMY_INFO = new AudioTrackInfoBuilder()
      .with(coreSourceName("undefined"))
      .with(coreIdentifier("direct"))
      .with(coreLength(Long.MAX_VALUE))
      .with(coreIsStream(true))
      .build();

  public static AudioTrackRequest create(AudioPlayback playback) {
    return new AudioTrackRequestBuilder(DUMMY_INFO)
        .withSourceManager(new Manager(playback));
  }

  private static class Manager implements AudioSource {
    private final AudioPlayback playback;

    private Manager(AudioPlayback playback) {
      this.playback = playback;
    }

    @Override
    public String getName() {
      return "direct";
    }

    @Override
    public AudioInfoEntity loadItem(AudioInfoRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
      return playback;
    }

    @Override
    public void close() {

    }
  }
}
