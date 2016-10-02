package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

public class AudioPlayerContext {
  public final AudioPlayer player;
  public final TrackScheduler scheduler;

  public AudioPlayerContext(AudioPlayer player, TrackScheduler scheduler) {
    this.player = player;
    this.scheduler = scheduler;
  }
}
