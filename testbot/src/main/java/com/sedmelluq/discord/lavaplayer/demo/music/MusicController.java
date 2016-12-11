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
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.DecodedTrackHolder;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.iharder.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
  private void serialize(Message message) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MessageOutput outputStream = new MessageOutput(baos);

    for (AudioTrack track : scheduler.drainQueue()) {
      manager.encodeTrack(outputStream, track);
    }

    outputStream.finish();

    message.getChannel().sendMessage(Base64.encodeBytes(baos.toByteArray())).queue();
  }

  @BotCommandHandler
  private void deserialize(Message message, String content) throws IOException {
    outputChannel.set((TextChannel) message.getChannel());
    connectToFirstVoiceChannel(guild.getAudioManager());

    byte[] bytes = Base64.decode(content);

    MessageInput inputStream = new MessageInput(new ByteArrayInputStream(bytes));
    DecodedTrackHolder holder;

    while ((holder = manager.decodeTrack(inputStream)) != null) {
      if (holder.decodedTrack != null) {
        scheduler.addToQueue(holder.decodedTrack);
      }
    }
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
      message.getChannel().sendMessage("Duration is " + track.getDuration()).queue();
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
      message.getChannel().sendMessage("Position is " + track.getPosition()).queue();
    });
  }

  @BotCommandHandler
  private void marker(final Message message, long position, final String text) {
    forPlayingTrack(track -> {
      track.setMarker(new TrackMarker(position, state -> {
        message.getChannel().sendMessage("Trigger [" + text + "] cause [" + state.name() + "]").queue();
      }));
    });
  }

  @BotCommandHandler
  private void unmark(Message message) {
    forPlayingTrack(track -> {
      track.setMarker(null);
    });
  }

  private void addTrack(final Message message, final String identifier, final boolean now) {
    outputChannel.set((TextChannel) message.getChannel());

    manager.loadItemOrdered(this, identifier, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        connectToFirstVoiceChannel(guild.getAudioManager());

        message.getChannel().sendMessage("Starting now: " + track.getInfo().title + " (length " + track.getDuration() + ")").queue();

        if (now) {
          scheduler.playNow(track, true);
        } else {
          scheduler.addToQueue(track);
        }
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        List<AudioTrack> tracks = playlist.getTracks();
        message.getChannel().sendMessage("Loaded playlist: " + playlist.getName() + " (" + tracks.size() + ")").queue();

        connectToFirstVoiceChannel(guild.getAudioManager());

        AudioTrack selected = playlist.getSelectedTrack();

        if (selected != null) {
          message.getChannel().sendMessage("Selected track from playlist: " + selected.getInfo().title).queue();
        } else {
          selected = tracks.get(0);
          message.getChannel().sendMessage("Added first track from playlist: " + selected.getInfo().title).queue();
        }

        if (now) {
          scheduler.playNow(selected, true);
        } else {
          scheduler.addToQueue(selected);
        }

        for (int i = 0; i < Math.min(10, playlist.getTracks().size()); i++) {
          if (tracks.get(i) != selected) {
            scheduler.addToQueue(tracks.get(i));
          }
        }
      }

      @Override
      public void noMatches() {
        message.getChannel().sendMessage("Nothing found for " + identifier).queue();
      }

      @Override
      public void loadFailed(FriendlyException throwable) {
        message.getChannel().sendMessage("Failed with message: " + throwable.getMessage() + " (" + throwable.getClass().getSimpleName() + ")").queue();
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
    public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure) {
      TextChannel channel = outputChannel.get();

      if (channel != null) {
        channel.sendMessage(message).queue(success, failure);
      }
    }

    @Override
    public void sendMessage(String message) {
      TextChannel channel = outputChannel.get();

      if (channel != null) {
        channel.sendMessage(message).queue();
      }
    }
  }

  private final class FixedDispatcher implements MessageDispatcher {
    private final TextChannel channel;

    private FixedDispatcher(TextChannel channel) {
      this.channel = channel;
    }

    @Override
    public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure) {
      channel.sendMessage(message).queue(success, failure);
    }

    @Override
    public void sendMessage(String message) {
      channel.sendMessage(message).queue();
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
