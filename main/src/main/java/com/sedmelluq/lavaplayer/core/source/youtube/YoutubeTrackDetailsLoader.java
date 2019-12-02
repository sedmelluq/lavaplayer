package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;

public interface YoutubeTrackDetailsLoader {
  YoutubeTrackDetails loadDetails(HttpInterface httpInterface, String videoId, AudioTrackInfoTemplate template);
}
