package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.ExtendedHttpConfigurable;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;

public interface YoutubeSearchMusicResultLoader {
  AudioInfoEntity loadSearchMusicResult(String query, AudioTrackInfoTemplate template);

  ExtendedHttpConfigurable getHttpConfiguration();
}
