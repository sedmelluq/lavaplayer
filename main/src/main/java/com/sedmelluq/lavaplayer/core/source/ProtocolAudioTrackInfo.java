package com.sedmelluq.lavaplayer.core.source;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.ExtendedAudioTrackInfo;

public class ProtocolAudioTrackInfo extends ExtendedAudioTrackInfo {
  public ProtocolAudioTrackInfo(AudioTrackInfo delegate) {
    super(delegate);
  }

  @Override
  public String getUri() {
    return super.getUri() != null ? super.getUri() : getIdentifier();
  }
}
