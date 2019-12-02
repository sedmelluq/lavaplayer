package com.sedmelluq.lavaplayer.core.info.request;

import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoResponseHandler;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import java.util.Map;

public interface AudioInfoRequest extends AudioTrackInfoTemplate, AudioInfoEntity {
  String name();

  AudioInfoResponseHandler getResponseHandler();

  boolean isSourceAllowed(AudioSource source);

  Object getOrderChannelKey();

  <T> T getCustomOption(String name, Class<T> klass);

  Map<String, Object> getCustomOptions();
}
