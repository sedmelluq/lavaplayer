package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class TrackScheduler extends AudioEventAdapter {
  private final Queue<AudioTrack> tracks = new LinkedBlockingDeque<>();

  public final AudioPlayer player;

  public TrackScheduler(AudioPlayer player) {
    this.player = player;

    player.addListener(this);
  }

  public void skipTrack() {
    playNextTrack();
  }

  public void enqueue(AudioTrack track) {
    if (tracks.isEmpty() && player.startTrack(track, true)) {
      return;
    }

    tracks.add(track);
  }

  public void playNow(AudioTrack track) {
    tracks.clear();
    player.startTrack(track, false);
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, boolean interrupted) {
    if (!interrupted) {
      playNextTrack();
    }
  }

  @Override
  public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    playNextTrack();
  }

  private void playNextTrack() {
    AudioTrack nextTrack = tracks.poll();

    if (nextTrack != null) {
      player.playTrack(nextTrack);
    }
  }
}
