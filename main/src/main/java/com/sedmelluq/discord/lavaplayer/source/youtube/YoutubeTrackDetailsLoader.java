package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

public interface YoutubeTrackDetailsLoader {
  YoutubeTrackDetails loadDetails(HttpInterface httpInterface, String videoId, boolean requireFormats);
}
