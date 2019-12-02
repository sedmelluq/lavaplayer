package com.sedmelluq.lavaplayer.core.info.track;

import com.sedmelluq.lavaplayer.core.info.property.AbstractAudioTrackInfoPropertyHolder;
import com.sedmelluq.lavaplayer.core.info.property.AudioTrackCoreProperty;
import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import java.util.Map;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.METADATA_CORE;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.METADATA_EXTENDED;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.PLAYBACK_CORE;

public class CoreAudioTrackInfo extends AbstractAudioTrackInfoPropertyHolder implements AudioTrackInfo {
  public CoreAudioTrackInfo(Map<String, AudioTrackProperty> properties) {
    super(properties);
  }

  @Override
  public String getSourceName() {
    return coreString(AudioTrackCoreProperty.SOURCE);
  }

  @Override
  public String getIdentifier() {
    return coreString(AudioTrackCoreProperty.IDENTIFIER);
  }

  @Override
  public String getTitle() {
    return coreString(AudioTrackCoreProperty.TITLE);
  }

  @Override
  public String getAuthor() {
    return coreString(AudioTrackCoreProperty.AUTHOR);
  }

  @Override
  public long getLength() {
    return coreLong(AudioTrackCoreProperty.LENGTH);
  }

  @Override
  public boolean isStream() {
    return coreLong(AudioTrackCoreProperty.IS_STREAM) != 0;
  }

  @Override
  public String getUri() {
    return coreString(AudioTrackCoreProperty.URI);
  }

  @Override
  public String getArtworkUrl() {
    return coreString(AudioTrackCoreProperty.ARTWORK);
  }

  private String coreString(AudioTrackCoreProperty property) {
    return getStringProperty(property.name);
  }

  private long coreLong(AudioTrackCoreProperty property) {
    return getLongProperty(property.name);
  }
}
