package com.sedmelluq.lavaplayer.core.source.local;

import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import java.io.File;

public class LocalAudioPlayback implements AudioPlayback {
  private final AudioTrackInfo trackInfo;
  private final MediaContainerProbe containerProbe;

  public LocalAudioPlayback(AudioTrackInfo trackInfo, MediaContainerProbe containerProbe) {
    this.trackInfo = trackInfo;
    this.containerProbe = containerProbe;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    File file = new File(trackInfo.getIdentifier());

    try (LocalSeekableInputStream stream = new LocalSeekableInputStream(file)) {
      containerProbe.createPlayback(trackInfo, stream).process(controller);
    }
  }
}
