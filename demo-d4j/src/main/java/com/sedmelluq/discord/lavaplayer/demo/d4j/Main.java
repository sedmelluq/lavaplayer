package com.sedmelluq.discord.lavaplayer.demo.d4j;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.VoiceChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    DiscordClient client = new DiscordClientBuilder(System.getProperty("botToken")).build();
    new Main().registerListeners(client.getEventDispatcher());
    client.login().block();
  }

  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers;

  private Main() {
    this.musicManagers = new ConcurrentHashMap<>();

    this.playerManager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
  }

  private void registerListeners(EventDispatcher eventDispatcher) {
    eventDispatcher.on(MessageCreateEvent.class).subscribe(this::onMessageReceived);
  }

  private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
    long guildId = guild.getId().asLong();
    GuildMusicManager musicManager = musicManagers.get(guildId);

    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager);
      musicManagers.put(guildId, musicManager);
    }

    return musicManager;
  }

  private void onMessageReceived(MessageCreateEvent event) {
    Message message = event.getMessage();

    message.getContent().ifPresent(it -> {
      MessageChannel channel = message.getChannel().block();

      if (channel instanceof TextChannel) {
        String[] command = it.split(" ", 2);

        if ("~play".equals(command[0]) && command.length == 2) {
          loadAndPlay((TextChannel) channel, command[1]);
        } else if ("~skip".equals(command[0])) {
          skipTrack((TextChannel) channel);
        }
      }
    });
  }

  private void loadAndPlay(TextChannel channel, final String trackUrl) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild().block());

    playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        sendMessageToChannel(channel, "Adding to queue " + track.getInfo().title);

        play(channel.getGuild().block(), musicManager, track);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack firstTrack = playlist.getSelectedTrack();

        if (firstTrack == null) {
          firstTrack = playlist.getTracks().get(0);
        }

        sendMessageToChannel(channel, "Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")");

        play(channel.getGuild().block(), musicManager, firstTrack);
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

  private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
    GuildMusicManager manager = getGuildAudioPlayer(guild);
    attachToFirstVoiceChannel(guild, manager.provider);
    musicManager.scheduler.queue(track);
  }

  private void skipTrack(TextChannel channel) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild().block());
    musicManager.scheduler.nextTrack();

    sendMessageToChannel(channel, "Skipped to next track.");
  }

  private void sendMessageToChannel(TextChannel channel, String message) {
    try {
      channel.createMessage(message).block();
    } catch (Exception e) {
      log.warn("Failed to send message {} to {}", message, channel.getName(), e);
    }
  }

  private static void attachToFirstVoiceChannel(Guild guild, D4jAudioProvider provider) {
    VoiceChannel voiceChannel = guild.getChannels().ofType(VoiceChannel.class).blockFirst();
    boolean inVoiceChannel = guild.getVoiceStates() // Check if any VoiceState for this guild relates to bot
        .any(voiceState -> guild.getClient().getSelfId().map(voiceState.getUserId()::equals).orElse(false))
        .block();

    if (!inVoiceChannel) {
      voiceChannel.join(spec -> spec.setProvider(provider)).block();
    }
  }
}
