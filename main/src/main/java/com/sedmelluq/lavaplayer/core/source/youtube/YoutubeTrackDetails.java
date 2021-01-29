package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.source.youtube.signature.YoutubeSignatureResolver;
import java.util.List;

public interface YoutubeTrackDetails {
  AudioTrackInfo getTrackInfo();

  List<YoutubeTrackFormat> getFormats(HttpInterface httpInterface, YoutubeSignatureResolver signatureResolver);

  String getPlayerScript();
}
