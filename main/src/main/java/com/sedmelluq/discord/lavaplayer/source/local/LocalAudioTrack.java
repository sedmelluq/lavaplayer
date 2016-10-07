package com.sedmelluq.discord.lavaplayer.source.local;

import com.sedmelluq.discord.lavaplayer.container.matroska.MatroskaAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

/**
 * Audio track that handles processing local files as audio tracks.
 */
public class LocalAudioTrack extends DelegatedAudioTrack {
  private final File file;

  /**
   * @param manager Audio player manager which created the track
   * @param executor Track executor
   * @param trackInfo Track info
   */
  public LocalAudioTrack(AudioPlayerManager manager, AudioTrackExecutor executor, AudioTrackInfo trackInfo) {
    super(manager, executor, trackInfo);

    this.file = new File(executor.getIdentifier());
  }

  @Override
  public void process(AtomicInteger volumeLevel) throws Exception {
    if (executor.getIdentifier().endsWith(".mp4") || executor.getIdentifier().endsWith(".m4a")) {
      processDelegate(new MpegAudioTrack(manager, executor, trackInfo, new LocalSeekableInputStream(file)), volumeLevel);
    } else if (executor.getIdentifier().endsWith(".webm") || executor.getIdentifier().endsWith(".mkv")) {
      processDelegate(new MatroskaAudioTrack(manager, executor, trackInfo, new LocalSeekableInputStream(file)), volumeLevel);
    } else if (executor.getIdentifier().endsWith(".mp3")) {
      processDelegate(new Mp3AudioTrack(manager, executor, trackInfo, new LocalSeekableInputStream(file)), volumeLevel);
    } else {
      throw new FriendlyException("Unknown file extension.", COMMON, null);
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new LocalAudioTrack(manager, new AudioTrackExecutor(getIdentifier()), trackInfo);
  }
}
