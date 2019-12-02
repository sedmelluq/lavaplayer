package com.sedmelluq.lavaplayer.core.source;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.container.MediaContainerRegistry;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackCoreProperty.IDENTIFIER;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackCoreProperty.SOURCE;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.PLAYBACK_CORE;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreOrdered;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.custom;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.COMMON;

/**
 * The base class for audio sources which use probing to detect container type.
 */
public abstract class AbstractProtocolAudioSource implements AudioSource {
  private static final String CONTAINER_PROPERTY = "container";

  protected final MediaContainerRegistry containerRegistry;

  protected AbstractProtocolAudioSource(MediaContainerRegistry containerRegistry) {
    this.containerRegistry = containerRegistry;
  }

  protected AudioInfoEntity handleLoadResult(String identifier, MediaContainerDetectionResult result) {
    if (result != null) {
      if (result.isRequest()) {
        return result.getRequest();
      } else if (!result.isContainerDetected()) {
        throw new FriendlyException("Unknown file format.", COMMON, null);
      } else if (!result.isSupportedFile()) {
        throw new FriendlyException(result.getUnsupportedReason(), COMMON, null);
      } else {
        return result.getTrackInfoBuilder()
            .with(coreOrdered(SOURCE, getName(), -1))
            .with(coreOrdered(IDENTIFIER, identifier, -1))
            .with(custom(CONTAINER_PROPERTY, PLAYBACK_CORE.mask, result.getContainerProbe().getName()))
            .build();
      }
    }

    return null;
  }

  protected MediaContainerProbe detectProbe(AudioTrackInfo trackInfo) {
    String containerName = trackInfo.getStringProperty("container");
    MediaContainerProbe probe = containerRegistry.find(containerName);

    if (probe == null) {
      throw new FriendlyException("Track format " + containerName + " is not supported.", COMMON, null);
    }

    return probe;
  }
}
