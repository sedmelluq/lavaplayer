package com.sedmelluq.discord.lavaplayer.demo.music;

import com.sedmelluq.discord.lavaplayer.demo.BotApplicationManager;
import com.sedmelluq.discord.lavaplayer.demo.BotGuildContext;
import com.sedmelluq.discord.lavaplayer.demo.MessageDispatcher;
import com.sedmelluq.discord.lavaplayer.demo.controller.BotCommandHandler;
import com.sedmelluq.discord.lavaplayer.demo.controller.BotController;
import com.sedmelluq.discord.lavaplayer.demo.controller.BotControllerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioLoop;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.managers.AudioManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MusicController implements BotController {
  private final AudioPlayerManager manager;
  private final AudioPlayer player;
  private final AtomicReference<TextChannel> outputChannel;
  private final MusicScheduler scheduler;
  private final MessageDispatcher messageDispatcher;
  private final Guild guild;

  public MusicController(BotApplicationManager manager, BotGuildContext state, Guild guild) {
    this.manager = manager.getPlayerManager();
    this.guild = guild;

    player = manager.getPlayerManager().createPlayer();
    guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

    outputChannel = new AtomicReference<>();

    messageDispatcher = new GlobalDispatcher();
    scheduler = new MusicScheduler(player, messageDispatcher, manager.getExecutorService());

    player.addListener(scheduler);
  }

  @BotCommandHandler
  private void add(Message message, String identifier) {
    addTrack(message, identifier, false);
  }

  @BotCommandHandler
  private void now(Message message, String identifier) {
    addTrack(message, identifier, true);
  }

  @BotCommandHandler
  private void volume(Message message, int volume) {
    player.setVolume(volume);
  }

  @BotCommandHandler
  private void gc(Message message, int duration) {
    UdpQueueManager.pauseDemo(duration);
  }

  @BotCommandHandler
  private void skip(Message message) {
    scheduler.skip();
  }

  @BotCommandHandler
  private void forward(Message message, int duration) {
    forPlayingTrack(track -> {
      track.setPosition(track.getPosition() + duration);
    });
  }

  @BotCommandHandler
  private void back(Message message, int duration) {
    forPlayingTrack(track -> {
      track.setPosition(Math.max(0, track.getPosition() - duration));
    });
  }

  @BotCommandHandler
  private void pause(Message message) {
    player.setPaused(true);
  }

  @BotCommandHandler
  private void resume(Message message) {
    player.setPaused(false);
  }

  @BotCommandHandler
  private void duration(Message message) {
    forPlayingTrack(track -> {
      message.getChannel().sendMessage("Duration is " + track.getDuration());
    });
  }

  @BotCommandHandler
  private void seek(Message message, long position) {
    forPlayingTrack(track -> {
      track.setPosition(position);
    });
  }

  @BotCommandHandler
  private void pos(Message message) {
    forPlayingTrack(track -> {
      message.getChannel().sendMessage("Position is " + track.getPosition());
    });
  }

  @BotCommandHandler
  private void loop(Message message, long start, long end) {
    forPlayingTrack(track -> {
      track.setLoop(new AudioLoop(start, end));
    });
  }

  @BotCommandHandler
  private void cancel(Message message) {
    forPlayingTrack(track -> {
      track.setLoop(null);
    });
  }

  private void addTrack(final Message message, final String identifier, final boolean now) {
    outputChannel.set((TextChannel) message.getChannel());

    manager.loadItemOrdered(this, identifier, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        connectToFirstVoiceChannel(guild.getAudioManager());

        message.getChannel().sendMessage("Starting now: " + track.getInfo().title + " (length " + track.getDuration() + ")");

        if (now) {
          scheduler.playNow(track, true);
        } else {
          scheduler.addToQueue(track);
        }
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        List<AudioTrack> tracks = playlist.getTracks();
        message.getChannel().sendMessage("Loaded playlist: " + playlist.getName() + " (" + tracks.size() + ")");

        AudioTrack selected = playlist.getSelectedTrack();
        if (selected != null) {
          message.getChannel().sendMessage("Selected track from playlist: " + selected.getInfo().title);
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
        message.getChannel().sendMessage("Nothing found for " + identifier);
      }

      @Override
      public void loadFailed(FriendlyException throwable) {
        message.getChannel().sendMessage("Failed with message: " + throwable.getMessage() + " (" + throwable.getClass().getSimpleName() + ")");
      }
    });
  }

  private void forPlayingTrack(TrackOperation operation) {
    AudioTrack track = player.getPlayingTrack();

    if (track != null) {
      operation.execute(track);
    }
  }

  private static void connectToFirstVoiceChannel(AudioManager audioManager) {
    if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
      for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
        audioManager.openAudioConnection(voiceChannel);
        break;
      }
    }
  }

  private interface TrackOperation {
    void execute(AudioTrack track);
  }

  private class GlobalDispatcher implements MessageDispatcher {
    @Override
    public Message sendMessage(String message) {
      TextChannel channel = outputChannel.get();

      if (channel != null) {
        return channel.sendMessage(message);
      } else {
        return null;
      }
    }
  }

  private final class FixedDispatcher implements MessageDispatcher {
    private final TextChannel channel;

    private FixedDispatcher(TextChannel channel) {
      this.channel = channel;
    }

    @Override
    public Message sendMessage(String message) {
      return channel.sendMessage(message);
    }
  }

  public static class Factory implements BotControllerFactory<MusicController> {
    @Override
    public Class<MusicController> getControllerClass() {
      return MusicController.class;
    }

    @Override
    public MusicController create(BotApplicationManager manager, BotGuildContext state, Guild guild) {
      return new MusicController(manager, state, guild);
    }
  }
}
