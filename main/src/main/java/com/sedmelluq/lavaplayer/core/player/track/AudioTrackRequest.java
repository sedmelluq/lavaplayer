package com.sedmelluq.lavaplayer.core.player.track;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.marker.TrackMarker;
import com.sedmelluq.lavaplayer.core.source.AudioSource;

public interface AudioTrackRequest {
  AudioTrackInfo getTrackInfo();

  AudioSource getSource();

  long getInitialPosition();

  TrackMarker getInitialMarker();

  Object getUserData();

  boolean getReplaceExisting();
}
