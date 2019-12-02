package com.sedmelluq.lavaplayer.core.source.http;

import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.http.PersistentHttpStream;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUrlPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(HttpUrlPlayback.class);

  private final AudioTrackInfo trackInfo;
  private final MediaContainerProbe containerProbe;
  private final HttpInterfaceManager httpInterfaceManager;

  public HttpUrlPlayback(
      AudioTrackInfo trackInfo,
      MediaContainerProbe containerProbe,
      HttpInterfaceManager httpInterfaceManager
  ) {
    this.trackInfo = trackInfo;
    this.containerProbe = containerProbe;
    this.httpInterfaceManager = httpInterfaceManager;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    String url = trackInfo.getIdentifier();

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      log.debug("Starting http track from URL: {}", url);

      try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, URI.create(url), Long.MAX_VALUE)) {
        containerProbe.createPlayback(trackInfo, stream).process(controller);
      }
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }
}
