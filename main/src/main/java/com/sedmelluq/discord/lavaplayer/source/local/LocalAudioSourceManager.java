package com.sedmelluq.discord.lavaplayer.source.local;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;

import java.io.File;

/**
 * Audio source manager that implements finding audio files from the local file system.
 */
public class LocalAudioSourceManager implements AudioSourceManager {
  @Override
  public InternalAudioTrack loadItem(AudioPlayerManager manager, String identifier) {
    if (new File(identifier).exists()) {
      return new LocalAudioTrack(new AudioTrackInfo(identifier, "Unknown", 0, identifier));
    } else {
      return null;
    }
  }
}
