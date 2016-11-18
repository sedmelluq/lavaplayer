package com.sedmelluq.discord.lavaplayer.source;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

/**
 * The base class for audio sources which use probing to detect container type.
 */
public abstract class ProbingAudioSourceManager implements AudioSourceManager {
  protected AudioItem handleLoadResult(MediaContainerDetectionResult result) {
    if (result != null) {
      if (result.isReference()) {
        return result.getReference();
      } else if (!result.isContainerDetected()) {
        throw new FriendlyException("Unknown file format.", COMMON, null);
      } else if (!result.isSupportedFile()) {
        throw new FriendlyException(result.getUnsupportedReason(), COMMON, null);
      } else {
        return createTrack(result.getTrackInfo(), result.getContainerProbe());
      }
    }

    return null;
  }

  protected abstract AudioTrack createTrack(AudioTrackInfo trackInfo, MediaContainerProbe probe);
}
