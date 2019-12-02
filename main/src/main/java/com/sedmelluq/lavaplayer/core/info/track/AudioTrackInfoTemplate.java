package com.sedmelluq.lavaplayer.core.info.track;

import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import java.util.List;

public interface AudioTrackInfoTemplate {
  int getPropertyFlagMask();

  List<AudioTrackProperty> getProperties();
}
