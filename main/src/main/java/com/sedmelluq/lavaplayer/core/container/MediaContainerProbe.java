package com.sedmelluq.lavaplayer.core.container;

import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrack;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;

/**
 * Track information probe for one media container type and factory for tracks for that container.
 */
public interface MediaContainerProbe {
  /**
   * @return The name of this container
   */
  String getName();

  /**
   * @param hints The available hints about the possible container.
   * @return True if the hints match the format this probe detects. Should always return false if all hints are null.
   */
  boolean matchesHints(MediaContainerHints hints);

  /**
   * Detect whether the file readable from the input stream is using this container and if this specific file uses
   * a format and codec that is supported for playback.
   *
   * @param inputStream Input stream that contains the track file
   * @return Returns result with audio track on supported format, result with unsupported reason set if this is the
   *         container that the file uses, but this specific file uses a format or codec that is not supported. Returns
   *         null in case this file does not appear to be using this container format.
   * @throws IOException On read error.
   */
  MediaContainerDetectionResult probe(AudioInfoRequest request, SeekableInputStream inputStream) throws IOException;

  /**
   * Creates a new track for this container. The audio tracks created here are never used directly, but the playback is
   * delegated to them. As such, they do not have to support cloning or have a source manager.
   *
   * @param inputStream Input stream of the track file
   * @return A new audio track
   */
  AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream);
}
