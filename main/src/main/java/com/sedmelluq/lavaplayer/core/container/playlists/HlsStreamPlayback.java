package com.sedmelluq.lavaplayer.core.container.playlists;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;

public class HlsStreamPlayback extends MpegTsM3uStreamPlayback {
  private final HlsStreamSegmentUrlProvider segmentUrlProvider;
  private final HttpInterfaceManager httpInterfaceManager;

  public HlsStreamPlayback(
      String streamUrl,
      HttpInterfaceManager httpInterfaceManager,
      boolean isInnerUrl
  ) {
    super(streamUrl);

    segmentUrlProvider = isInnerUrl ?
        new HlsStreamSegmentUrlProvider(null, streamUrl) :
        new HlsStreamSegmentUrlProvider(streamUrl, null);

    this.httpInterfaceManager = httpInterfaceManager;
  }

  @Override
  protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
    return segmentUrlProvider;
  }

  @Override
  protected HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }
}
