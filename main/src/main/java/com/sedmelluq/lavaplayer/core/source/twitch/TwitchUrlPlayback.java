package com.sedmelluq.lavaplayer.core.source.twitch;

import com.sedmelluq.lavaplayer.core.container.playlists.M3uStreamSegmentUrlProvider;
import com.sedmelluq.lavaplayer.core.container.playlists.MpegTsM3uStreamPlayback;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitchUrlPlayback extends MpegTsM3uStreamPlayback {
  private static final Logger log = LoggerFactory.getLogger(TwitchUrlPlayback.class);

  private final String url;
  private final TwitchStreamAudioSource sourceManager;
  private final M3uStreamSegmentUrlProvider segmentUrlProvider;

  public TwitchUrlPlayback(String url, TwitchStreamAudioSource sourceManager) {
    super(url);

    this.url = url;
    this.sourceManager = sourceManager;
    this.segmentUrlProvider = new TwitchStreamSegmentUrlProvider(getChannelName(), sourceManager);
  }

  /**
   * @return Name of the channel of the stream.
   */
  public String getChannelName() {
    return TwitchStreamAudioSource.getChannelIdentifierFromUrl(url);
  }

  @Override
  protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
    return segmentUrlProvider;
  }

  @Override
  protected HttpInterface getHttpInterface() {
    return sourceManager.getHttpInterface();
  }

  @Override
  public void process(AudioPlaybackController controller) {
    log.debug("Starting to play Twitch channel {}.", getChannelName());

    super.process(controller);
  }
}
