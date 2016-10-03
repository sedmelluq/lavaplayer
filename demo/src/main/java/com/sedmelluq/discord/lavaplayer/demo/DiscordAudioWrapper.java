package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.audio.AudioSendHandler;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.managers.AudioManager;

public class DiscordAudioWrapper implements AudioSendHandler {
  private final TrackScheduler scheduler;
  private AudioFrame activeFrame;
  private AudioFrame lastProvidedFrame;

  public DiscordAudioWrapper(TrackScheduler scheduler) {
    this.scheduler = scheduler;
  }

  public AudioFrame getCurrentFrame() {
    if (activeFrame == null) {
      activeFrame = scheduler.player.provide();
    }
    return activeFrame;
  }

  @Override
  public boolean canProvide() {
    AudioFrame frame = getCurrentFrame();
    return frame != null;
  }

  @Override
  public byte[] provide20MsAudio() {
    AudioFrame frame = getCurrentFrame();

    activeFrame = null;
    lastProvidedFrame = frame;

    return frame.data;
  }

  @Override
  public boolean isOpus() {
    return lastProvidedFrame != null;
  }

  private static TrackScheduler getExistingPlayerForGuild(Guild guild) {
    AudioManager audioManager = guild.getAudioManager();
    AudioSendHandler sendHandler = audioManager.getSendingHandler();

    return sendHandler instanceof DiscordAudioWrapper ? ((DiscordAudioWrapper) sendHandler).scheduler : null;
  }

  public static TrackScheduler getSchedulerForGuild(AudioPlayerManager audioPlayerManager, Guild guild) {
    TrackScheduler scheduler = getExistingPlayerForGuild(guild);

    if (scheduler == null) {
      AudioManager audioManager = guild.getAudioManager();
      scheduler = new TrackScheduler(audioPlayerManager.createPlayer());
      audioManager.setSendingHandler(new DiscordAudioWrapper(scheduler));
    }

    return scheduler;
  }

  public static void clearForGuild(Guild guild) {
    TrackScheduler scheduler = getExistingPlayerForGuild(guild);

    if (scheduler != null) {
      scheduler.player.destroy();
      guild.getAudioManager().setSendingHandler(null);
    }
  }
}
