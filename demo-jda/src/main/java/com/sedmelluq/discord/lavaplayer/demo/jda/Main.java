package com.sedmelluq.discord.lavaplayer.demo.jda;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.HashMap;
import java.util.Map;

public class Main extends ListenerAdapter {
  public static void main(String[] args) throws Exception {
    JDA jda = new JDABuilder(AccountType.BOT)
        .setToken(System.getProperty("botToken"))
        .buildBlocking();

    jda.addEventListener(new Main());
  }

  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers;

  private Main() {
    this.musicManagers = new HashMap<>();

    this.playerManager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
  }

  private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
    long guildId = Long.parseLong(guild.getId());
    GuildMusicManager musicManager = musicManagers.get(guildId);

    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager);
      musicManagers.put(guildId, musicManager);
    }

    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

    return musicManager;
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    String[] command = event.getMessage().getContent().split(" ", 2);
    Guild guild = event.getGuild();

    if (guild != null) {
      if ("~play".equals(command[0]) && command.length == 2) {
        loadAndPlay(event.getTextChannel(), command[1]);
      } else if ("~skip".equals(command[0])) {
        skipTrack(event.getTextChannel());
      }
    }

    super.onMessageReceived(event);
  }

  private void loadAndPlay(final TextChannel channel, final String trackUrl) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

    playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

        play(channel.getGuild(), musicManager, track);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack firstTrack = playlist.getSelectedTrack();

        if (firstTrack == null) {
          firstTrack = playlist.getTracks().get(0);
        }

        channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

        play(channel.getGuild(), musicManager, firstTrack);
      }

      @Override
      public void noMatches() {
        channel.sendMessage("Nothing found by " + trackUrl).queue();
      }

      @Override
      public void loadFailed(FriendlyException exception) {
        channel.sendMessage("Could not play: " + exception.getMessage()).queue();
      }
    });
  }

  private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
    connectToFirstVoiceChannel(guild.getAudioManager());

    musicManager.scheduler.queue(track);
  }

  private void skipTrack(TextChannel channel) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    musicManager.scheduler.nextTrack();

    channel.sendMessage("Skipped to next track.").queue();
  }

  private static void connectToFirstVoiceChannel(AudioManager audioManager) {
    if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
      for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
        audioManager.openAudioConnection(voiceChannel);
        break;
      }
    }
  }
}
