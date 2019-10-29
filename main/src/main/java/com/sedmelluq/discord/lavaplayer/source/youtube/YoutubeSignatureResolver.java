package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import java.net.URI;

public interface YoutubeSignatureResolver {
  URI resolveFormatUrl(HttpInterface httpInterface, String playerScript, YoutubeTrackFormat format) throws Exception;

  String resolveDashUrl(HttpInterface httpInterface, String playerScript, String dashUrl) throws Exception;
}
