package com.sedmelluq.lavaplayer.core.player.track;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.marker.TrackMarker;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.tools.userdata.UserDataHolder;

public interface AudioTrack extends UserDataHolder {
  AudioTrackInfo getInfo();

  AudioTrackState getState();

  boolean isSeekable();

  long getPosition();

  void setPosition(long position);

  void setMarker(TrackMarker marker);

  long getDuration();

  AudioSource getSource();
}
