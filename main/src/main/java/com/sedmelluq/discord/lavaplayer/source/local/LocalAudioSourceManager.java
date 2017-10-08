package com.sedmelluq.discord.lavaplayer.source.local;

import com.sedmelluq.discord.lavaplayer.container.*;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding audio files from the local file system.
 */
public class LocalAudioSourceManager extends ProbingAudioSourceManager {
  @Override
  public String getSourceName() {
    return "local";
  }

  @Override
  public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
    File file = new File(reference.identifier);

    if (file.exists() && file.isFile() && file.canRead()) {
      return handleLoadResult(detectContainerForFile(reference, file));
    } else {
      return null;
    }
  }

  @Override
  protected AudioTrack createTrack(AudioTrackInfo trackInfo, MediaContainerProbe probe) {
    return new LocalAudioTrack(trackInfo, probe, this);
  }

  private MediaContainerDetectionResult detectContainerForFile(AudioReference reference, File file) {
    try (LocalSeekableInputStream inputStream = new LocalSeekableInputStream(file)) {
      return MediaContainerDetection.detectContainer(reference, inputStream, MediaContainerHints.from(null, null));
    } catch (IOException e) {
      throw new FriendlyException("Failed to open file for reading.", SUSPICIOUS, e);
    }
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    output.writeUTF(((LocalAudioTrack) track).getProbe().getName());
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    String probeName = input.readUTF();

    for (MediaContainer container : MediaContainer.class.getEnumConstants()) {
      if (container.probe.getName().equals(probeName)) {
        return new LocalAudioTrack(trackInfo, container.probe, this);
      }
    }

    return null;
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
  }
}