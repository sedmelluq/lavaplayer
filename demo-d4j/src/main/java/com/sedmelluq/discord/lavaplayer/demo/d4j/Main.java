package com.sedmelluq.discord.lavaplayer.demo.d4j;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.audio.IAudioManager;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.MissingPermissionsException;

import java.util.HashMap;
import java.util.Map;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    IDiscordClient client = new ClientBuilder()
        .withToken(System.getProperty("botToken"))
        .login();

    client.getDispatcher().registerListener(new Main());
  }

  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers;

  private Main() {
    this.musicManagers = new HashMap<>();

    this.playerManager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
  }

  private synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
    long guildId = guild.getLongID();
    GuildMusicManager musicManager = musicManagers.get(guildId);

    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager);
      musicManagers.put(guildId, musicManager);
    }

    guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

    return musicManager;
  }

  @EventSubscriber
  public void onMessageReceived(MessageReceivedEvent event) {
    IMessage message = event.getMessage();

    String[] command = message.getContent().split(" ", 2);
    IGuild guild = message.getGuild();

    if (guild != null) {
      if ("~play".equals(command[0]) && command.length == 2) {
        loadAndPlay(message.getChannel(), command[1]);
      } else if ("~skip".equals(command[0])) {
        skipTrack(message.getChannel());
      }
    }
  }

  private void loadAndPlay(final IChannel channel, final String trackUrl) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

    playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        sendMessageToChannel(channel, "Adding to queue " + track.getInfo().title);

        play(channel.getGuild(), musicManager, track);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack firstTrack = playlist.getSelectedTrack();

        if (firstTrack == null) {
          firstTrack = playlist.getTracks().get(0);
        }

        sendMessageToChannel(channel, "Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")");

        play(channel.getGuild(), musicManager, firstTrack);
      }

      @Override
      public void noMatches() {
        sendMessageToChannel(channel, "Nothing found by " + trackUrl);
      }

      @Override
      public void loadFailed(FriendlyException exception) {
        sendMessageToChannel(channel, "Could not play: " + exception.getMessage());
      }
    });
  }

  private void play(IGuild guild, GuildMusicManager musicManager, AudioTrack track) {
    connectToFirstVoiceChannel(guild.getAudioManager());

    musicManager.scheduler.queue(track);
  }

  private void skipTrack(IChannel channel) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    musicManager.scheduler.nextTrack();

    sendMessageToChannel(channel, "Skipped to next track.");
  }

  private void sendMessageToChannel(IChannel channel, String message) {
    try {
      channel.sendMessage(message);
    } catch (Exception e) {
      log.warn("Failed to send message {} to {}", message, channel.getName(), e);
    }
  }

  private static void connectToFirstVoiceChannel(IAudioManager audioManager) {
    for (IVoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
      if (voiceChannel.isConnected()) {
        return;
      }
    }

    for (IVoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
      try {
        voiceChannel.join();
      } catch (MissingPermissionsException e) {
        log.warn("Cannot enter voice channel {}", voiceChannel.getName(), e);
      }
    }
  }
}
