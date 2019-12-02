package com.sedmelluq.lavaplayer.core.info.track;

import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.property.AudioTrackInfoPropertyHolder;

public interface AudioTrackInfo extends AudioTrackInfoPropertyHolder, AudioInfoEntity {
  String getSourceName();

  String getIdentifier();

  String getTitle();

  String getAuthor();

  long getLength();

  boolean isStream();

  String getUri();

  String getArtworkUrl();
}
