package com.sedmelluq.discord.lavaplayer.source.local;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding audio files from the local file system.
 */
public class LocalAudioSourceManager implements AudioSourceManager {
  @Override
  public String getSourceName() {
    return "local";
  }

  @Override
  public InternalAudioTrack loadItem(DefaultAudioPlayerManager manager, String identifier) {
    File file = new File(identifier);

    if (file.exists() && file.isFile() && file.canRead()) {
      MediaContainerDetection.Result result = detectContainerForFile(identifier, file);
      return new LocalAudioTrack(result.getTrackInfo(), result.getContainerProbe(), this);
    } else {
      return null;
    }
  }

  private MediaContainerDetection.Result detectContainerForFile(String identifier, File file) {
    try (LocalSeekableInputStream inputStream = new LocalSeekableInputStream(file)) {
      MediaContainerDetection.Result result = MediaContainerDetection.detectContainer(identifier, inputStream);

      if (!result.isContainerDetected()) {
        throw new FriendlyException("Unknown file format.", COMMON, null);
      } else if (!result.isSupportedFile()) {
        throw new FriendlyException(result.getUnsupportedReason(), COMMON, null);
      }

      return result;
    } catch (IOException e) {
      throw new FriendlyException("Failed to open file for reading.", SUSPICIOUS, e);
    }
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return false;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
  }
}
