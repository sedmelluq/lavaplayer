package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import java.net.URI;

public interface YoutubeSignatureResolver {
  URI resolveFormatUrl(HttpInterface httpInterface, String playerScript, YoutubeTrackFormat format) throws Exception;

  String resolveDashUrl(HttpInterface httpInterface, String playerScript, String dashUrl) throws Exception;
}
