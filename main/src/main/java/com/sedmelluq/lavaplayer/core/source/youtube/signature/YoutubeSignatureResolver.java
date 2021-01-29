package com.sedmelluq.lavaplayer.core.source.youtube.signature;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubeTrackFormat;
import java.net.URI;

public interface YoutubeSignatureResolver {
  URI resolveFormatUrl(HttpInterface httpInterface, String playerScript, YoutubeTrackFormat format) throws Exception;

  String resolveDashUrl(HttpInterface httpInterface, String playerScript, String dashUrl) throws Exception;
}
