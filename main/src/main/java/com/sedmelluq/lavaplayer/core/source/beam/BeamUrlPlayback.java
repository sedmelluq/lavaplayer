package com.sedmelluq.lavaplayer.core.source.beam;

import com.sedmelluq.lavaplayer.core.container.playlists.M3uStreamSegmentUrlProvider;
import com.sedmelluq.lavaplayer.core.container.playlists.MpegTsM3uStreamPlayback;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeamUrlPlayback extends MpegTsM3uStreamPlayback {
  private static final Logger log = LoggerFactory.getLogger(BeamUrlPlayback.class);

  private final String identifier;
  private final HttpInterfaceManager httpInterfaceManager;
  private final M3uStreamSegmentUrlProvider segmentUrlProvider;

  public BeamUrlPlayback(String identifier, HttpInterfaceManager httpInterfaceManager) {
    super(identifier);
    this.identifier = identifier;
    this.httpInterfaceManager = httpInterfaceManager;
    this.segmentUrlProvider = new BeamSegmentUrlProvider(getChannelId());
  }

  @Override
  protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
    return segmentUrlProvider;
  }

  @Override
  protected HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }

  @Override
  public void process(AudioPlaybackController controller) {
    log.debug("Starting to play Beam channel {}.", getChannelUrl());
    super.process(controller);
  }

  private String getChannelId() {
    return identifier.substring(0, identifier.indexOf('|'));
  }

  private String getChannelUrl() {
    return identifier.substring(identifier.lastIndexOf('|') + 1);
  }
}
