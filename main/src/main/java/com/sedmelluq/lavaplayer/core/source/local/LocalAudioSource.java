package com.sedmelluq.lavaplayer.core.source.local;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetection;
import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerHints;
import com.sedmelluq.lavaplayer.core.container.MediaContainerRegistry;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.request.generic.GenericAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.AbstractProtocolAudioSource;
import com.sedmelluq.lavaplayer.core.source.ProtocolAudioTrackInfo;
import java.io.File;

/**
 * Audio source manager that implements finding audio files from the local file system.
 */
public class LocalAudioSource extends AbstractProtocolAudioSource {
  public LocalAudioSource() {
    this(MediaContainerRegistry.DEFAULT_REGISTRY);
  }

  public LocalAudioSource(MediaContainerRegistry containerRegistry) {
    super(containerRegistry);
  }

  private MediaContainerDetectionResult detectContainerForFile(AudioInfoRequest request, File file) {
    try (LocalSeekableInputStream inputStream = new LocalSeekableInputStream(file)) {
      int lastDotIndex = file.getName().lastIndexOf('.');
      String fileExtension = lastDotIndex >= 0 ? file.getName().substring(lastDotIndex + 1) : null;

      return new MediaContainerDetection(containerRegistry, request, inputStream,
          MediaContainerHints.from(null, fileExtension)).detectContainer();
    }
  }

  @Override
  public String getName() {
    return "local";
  }

  @Override
  public AudioInfoEntity loadItem(AudioInfoRequest request) {
    if (request instanceof GenericAudioInfoRequest) {
      String path = ((GenericAudioInfoRequest) request).getHint();
      File file = new File(path);

      if (file.exists() && file.isFile() && file.canRead()) {
        return handleLoadResult(path, detectContainerForFile(request, file));
      }
    }

    return null;
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return new ProtocolAudioTrackInfo(trackInfo);
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new LocalAudioPlayback(trackInfo, detectProbe(trackInfo));
  }

  @Override
  public void close() {
    // Nothing to close.
  }
}
