package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

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
   * @param reference Reference with an identifier to use in the returned audio track info
   * @param inputStream Input stream that contains the track file
   * @return Returns result with audio track on supported format, result with unsupported reason set if this is the
   *         container that the file uses, but this specific file uses a format or codec that is not supported. Returns
   *         null in case this file does not appear to be using this container format.
   * @throws IOException On read error.
   */
  MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException;

  /**
   * Creates a new track for this container. The audio tracks created here are never used directly, but the playback is
   * delegated to them. As such, they do not have to support cloning or have a source manager.
   *
   * @param trackInfo Track meta information
   * @param inputStream Input stream of the track file
   * @return A new audio track
   */
  AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream);
}
