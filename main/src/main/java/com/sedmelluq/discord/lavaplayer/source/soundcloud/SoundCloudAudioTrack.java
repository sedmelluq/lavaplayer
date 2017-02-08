package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpAccessPoint;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Audio track that handles processing SoundCloud tracks.
 */
public class SoundCloudAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(SoundCloudAudioTrack.class);

  private final SoundCloudAudioSourceManager sourceManager;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public SoundCloudAudioTrack(AudioTrackInfo trackInfo, SoundCloudAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    String trackUrl = sourceManager.getTrackUrlFromId(trackInfo.identifier);

    try (HttpAccessPoint accessPoint = sourceManager.getAccessPoint()) {
      log.debug("Starting SoundCloud track from URL: {}", trackUrl);

      try (PersistentHttpStream stream = new PersistentHttpStream(accessPoint, new URI(trackUrl), null)) {
        processDelegate(new Mp3AudioTrack(trackInfo, stream), localExecutor);
      }
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new SoundCloudAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}
