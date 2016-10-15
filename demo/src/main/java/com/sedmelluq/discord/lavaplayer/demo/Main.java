package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioLoop;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    AudioPlayerManager playerManager = new AudioPlayerManager();
    //playerManager.useRemoteNodes("localhost:8080");
    playerManager.enableGcMonitoring();
    playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
    playerManager.registerSourceManager(new YoutubeAudioSourceManager());
    playerManager.registerSourceManager(new LocalAudioSourceManager());
    playerManager.registerSourceManager(new SoundCloudAudioSourceManager());

    JDA jda = new JDABuilder().setBotToken(System.getProperty("botToken")).buildBlocking();
    jda.addEventListener(new MessageListener(jda, playerManager));
  }

  private static class MessageListener extends ListenerAdapter {
    private final JDA jda;
    private final AudioPlayerManager playerManager;

    public MessageListener(JDA jda, AudioPlayerManager playerManager) {
      this.jda = jda;
      this.playerManager = playerManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
      if (!event.isPrivate()) {
        String message = event.getMessage().getContent();

        if (message.startsWith("~")) {
          Guild guild = event.getTextChannel().getGuild();
          TrackScheduler scheduler = DiscordAudioWrapper.getSchedulerForGuild(playerManager, guild);
          AudioPlayer player = scheduler.player;

          String[] parts = message.substring(1).split(" ");

          if ("next".equals(parts[0]) && parts.length > 1) {
            scheduler.setChannel(event.getTextChannel());
            executePlayCommand(event, message.split(" ", 2)[1], true);
          } else if ("add".equals(parts[0]) && parts.length > 1) {
            scheduler.setChannel(event.getTextChannel());
            executePlayCommand(event, message.split(" ", 2)[1], false);
          } else if ("volume".equals(parts[0]) && parts.length == 2) {
            player.setVolume(Integer.parseInt(parts[1]));
          } else if ("skip".equals(parts[0])) {
            scheduler.skipTrack();
          } else if ("ahead".equals(parts[0])) {
            AudioTrack track = player.getPlayingTrack();

            if (track != null) {
              track.setPosition(track.getPosition() + 10000);
            }
          } else if ("back".equals(parts[0])) {
            AudioTrack track = player.getPlayingTrack();

            if (track != null) {
              track.setPosition(track.getPosition() - 10000);
            }
          } else if ("pause".equals(parts[0])) {
            player.setPaused(true);
          } else if ("resume".equals(parts[0])) {
            player.setPaused(false);
          } else if ("duration".equals(parts[0])) {
            AudioTrack track = player.getPlayingTrack();

            if (track != null) {
              event.getTextChannel().sendMessage("Duration is: " + track.getDuration());
            }
          } else if ("pos".equals(parts[0]) && parts.length == 2) {
            AudioTrack track = player.getPlayingTrack();

            if (track != null) {
              track.setPosition(Long.parseLong(parts[1]));
            }
          } else if ("x".equals(parts[0])) {
            AudioTrack track = player.getPlayingTrack();

            if (track != null) {
              event.getTextChannel().sendMessage("Position is: " + track.getPosition());
            }
          } else if ("loop".equals(parts[0]) && parts.length == 3) {
            AudioTrack track = player.getPlayingTrack();

            if (track != null) {
              track.setLoop(new AudioLoop(Long.parseLong(parts[1]), Long.parseLong(parts[2])));
            }
          } else if ("cancel".equals(parts[0])) {
            AudioTrack track = player.getPlayingTrack();

            if (track != null) {
              track.setLoop(null);
            }
          }
        }
      }
    }

    private void executePlayCommand(final MessageReceivedEvent event, final String identifier, final boolean now) {
      final AudioManager audioManager = jda.getAudioManager(event.getGuild());
      final TrackScheduler scheduler = DiscordAudioWrapper.getSchedulerForGuild(playerManager, event.getGuild());

      playerManager.loadItem(identifier, new AudioLoadResultHandler() {
        @Override
        public void trackLoaded(AudioTrack track) {
          connectToFirstVoiceChannel(audioManager);

          if (now) {
            event.getTextChannel().sendMessage("Starting now: " + track.getInfo().title + " (length " + track.getDuration() + ")");
            scheduler.playNow(track);
          } else {
            event.getTextChannel().sendMessage("Adding to queue: " + track.getInfo().title + " (length " + track.getDuration() + ")");
            scheduler.enqueue(track);
          }
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
          List<AudioTrack> tracks = playlist.getTracks();
          event.getTextChannel().sendMessage("Loaded playlist: " + playlist.getName() + " (" + tracks.size() + ")");

          AudioTrack selected = playlist.getSelectedTrack();
          if (selected != null) {
            event.getTextChannel().sendMessage("Selected track from playlist: " + selected.getInfo().title);
            trackLoaded(selected);
          }

          for (int i = 0; i < Math.min(2, playlist.getTracks().size()); i++) {
            if (tracks.get(i) != selected) {
              trackLoaded(tracks.get(i));
            }
          }
        }

        @Override
        public void noMatches() {
          event.getTextChannel().sendMessage("Nothing found for " + identifier);
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
          event.getTextChannel().sendMessage("Failed with message: " + throwable.getMessage() + " (" + throwable.getClass().getSimpleName() + ")");
          log.warn("Failed to load track with identifier {} due to exception.", identifier, throwable);
        }
      });
    }

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
      if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
        for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
          audioManager.openAudioConnection(voiceChannel);
          break;
        }
      }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
      DiscordAudioWrapper.clearForGuild(event.getGuild());
    }
  }
}
