package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.container.mp3.Mp3StreamPlayback;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.PersistentHttpStream;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundCloudUrlPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(SoundCloudUrlPlayback.class);

  private final String identifier;
  private final SoundCloudAudioSourceManager sourceManager;

  public SoundCloudUrlPlayback(String identifier, SoundCloudAudioSourceManager sourceManager) {
    this.identifier = identifier;
    this.sourceManager = sourceManager;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      playFromIdentifier(httpInterface, identifier, false, controller);
    } catch (Exception e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private void playFromIdentifier(
      HttpInterface httpInterface,
      String identifier,
      boolean recursion,
      AudioPlaybackController controller
  ) throws Exception {
    SoundCloudM3uInfo m3uInfo = sourceManager.getFormatHandler().getM3uInfo(identifier);

    if (m3uInfo != null) {
      new SoundCloudOpusM3uUrlPlayback(identifier, httpInterface, m3uInfo).process(controller);
      return;
    }

    String mp3LookupUrl = sourceManager.getFormatHandler().getMp3LookupUrl(identifier);

    if (mp3LookupUrl != null) {
      String playbackUrl = SoundCloudHelper.loadPlaybackUrl(httpInterface, identifier.substring(2));
      loadFromMp3Url(controller, httpInterface, playbackUrl);
      return;
    }

    if (!recursion) {
      // Old "track ID" entry? Let's load it to get url.
      String lookupIdentifier = sourceManager.loadPlaybackIdentifier(httpInterface, identifier);
      playFromIdentifier(httpInterface, lookupIdentifier, true, controller);
    }
  }

  private void loadFromMp3Url(
      AudioPlaybackController controller,
      HttpInterface httpInterface,
      String trackUrl
  ) throws Exception {
    log.debug("Starting SoundCloud track from URL: {}", trackUrl);

    try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(trackUrl), null)) {
      if (!HttpClientTools.isSuccessWithContent(stream.checkStatusCode())) {
        throw new IOException("Invalid status code for soundcloud stream: " + stream.checkStatusCode());
      }

      new Mp3StreamPlayback(identifier, stream).process(controller);
    }
  }
}
