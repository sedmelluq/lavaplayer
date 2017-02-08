package com.sedmelluq.discord.lavaplayer.source.http;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Audio track that handles processing HTTP addresses as audio tracks.
 */
public class HttpAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(HttpAudioTrack.class);

  private final MediaContainerProbe probe;
  private final HttpAudioSourceManager sourceManager;

  /**
   * @param trackInfo Track info
   * @param probe Probe for the media container of this track
   * @param sourceManager Source manager used to load this track
   */
  public HttpAudioTrack(AudioTrackInfo trackInfo, MediaContainerProbe probe, HttpAudioSourceManager sourceManager) {
    super(trackInfo);

    this.probe = probe;
    this.sourceManager = sourceManager;
  }

  /**
   * @return The media probe which handles creating a container-specific delegated track for this track.
   */
  public MediaContainerProbe getProbe() {
    return probe;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      log.debug("Starting http track from URL: {}", trackInfo.identifier);

      try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, new URI(trackInfo.identifier), Long.MAX_VALUE)) {
        processDelegate((InternalAudioTrack) probe.createTrack(trackInfo, inputStream), localExecutor);
      }
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new HttpAudioTrack(trackInfo, probe, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}
