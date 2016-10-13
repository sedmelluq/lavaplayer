package com.sedmelluq.discord.lavaplayer.source.local;

import com.sedmelluq.discord.lavaplayer.container.matroska.MatroskaAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;

import java.io.File;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

/**
 * Audio track that handles processing local files as audio tracks.
 */
public class LocalAudioTrack extends DelegatedAudioTrack {
  private final File file;
  private final LocalAudioSourceManager sourceManager;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager used to load this track
   */
  public LocalAudioTrack(AudioTrackInfo trackInfo, LocalAudioSourceManager sourceManager) {
    super(trackInfo);

    this.file = new File(trackInfo.identifier);
    this.sourceManager = sourceManager;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    if (trackInfo.identifier.endsWith(".mp4") || trackInfo.identifier.endsWith(".m4a")) {
      processDelegate(new MpegAudioTrack(trackInfo, new LocalSeekableInputStream(file)), localExecutor);
    } else if (trackInfo.identifier.endsWith(".webm") || trackInfo.identifier.endsWith(".mkv")) {
      processDelegate(new MatroskaAudioTrack(trackInfo, new LocalSeekableInputStream(file)), localExecutor);
    } else if (trackInfo.identifier.endsWith(".mp3")) {
      processDelegate(new Mp3AudioTrack(trackInfo, new LocalSeekableInputStream(file)), localExecutor);
    } else {
      throw new FriendlyException("Unknown file extension.", COMMON, null);
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new LocalAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}
