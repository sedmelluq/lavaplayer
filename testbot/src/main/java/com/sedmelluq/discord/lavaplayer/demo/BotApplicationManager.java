package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.demo.controller.BotCommandMappingHandler;
import com.sedmelluq.discord.lavaplayer.demo.controller.BotController;
import com.sedmelluq.discord.lavaplayer.demo.controller.BotControllerManager;
import com.sedmelluq.discord.lavaplayer.demo.music.MusicController;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotApplicationManager extends ListenerAdapter {
  private static final Logger log = LoggerFactory.getLogger(BotApplicationManager.class);

  private final Map<Long, BotGuildContext> guildContexts;
  private final BotControllerManager controllerManager;
  private final AudioPlayerManager playerManager;
  private final ScheduledExecutorService executorService;

  public BotApplicationManager() {
    guildContexts = new HashMap<>();
    controllerManager = new BotControllerManager();

    controllerManager.registerController(new MusicController.Factory());

    playerManager = new DefaultAudioPlayerManager();
    //playerManager.useRemoteNodes("localhost:8080");
    playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
    playerManager.registerSourceManager(new YoutubeAudioSourceManager());
    playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
    playerManager.registerSourceManager(new BandcampAudioSourceManager());
    playerManager.registerSourceManager(new VimeoAudioSourceManager());
    playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
    playerManager.registerSourceManager(new BeamAudioSourceManager());
    playerManager.registerSourceManager(new HttpAudioSourceManager());
    playerManager.registerSourceManager(new LocalAudioSourceManager());

    executorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("bot"));
  }

  public ScheduledExecutorService getExecutorService() {
    return executorService;
  }

  public AudioPlayerManager getPlayerManager() {
    return playerManager;
  }

  private BotGuildContext createGuildState(long guildId, Guild guild) {
    BotGuildContext context = new BotGuildContext(guildId);

    for (BotController controller : controllerManager.createControllers(this, context, guild)) {
      context.controllers.put(controller.getClass(), controller);
    }

    return context;
  }

  private synchronized BotGuildContext getContext(Guild guild) {
    long guildId = Long.parseLong(guild.getId());
    BotGuildContext context = guildContexts.get(guildId);

    if (context == null) {
      context = createGuildState(guildId, guild);
      guildContexts.put(guildId, context);
    }

    return context;
  }

  @Override
  public void onMessageReceived(final MessageReceivedEvent event) {
    Member member = event.getMember();

    if (!event.isFromType(ChannelType.TEXT) || member == null || member.getUser().isBot()) {
      return;
    }

    BotGuildContext guildContext = getContext(event.getGuild());

    controllerManager.dispatchMessage(guildContext.controllers, "!/", event.getMessage(), new BotCommandMappingHandler() {
      @Override
      public void commandNotFound(Message message, String name) {

      }

      @Override
      public void commandWrongParameterCount(Message message, String name, String usage, int given, int required) {
        event.getTextChannel().sendMessage("Wrong argument count for command").queue();
      }

      @Override
      public void commandWrongParameterType(Message message, String name, String usage, int index, String value, Class<?> expectedType) {
        event.getTextChannel().sendMessage("Wrong argument type for command").queue();;
      }

      @Override
      public void commandRestricted(Message message, String name) {
        event.getTextChannel().sendMessage("Command not permitted").queue();;
      }

      @Override
      public void commandException(Message message, String name, Throwable throwable) {
        event.getTextChannel().sendMessage("Command threw an exception").queue();;

        log.error("Command with content {} threw an exception.", message.getContentDisplay(), throwable);
      }
    });
  }

  @Override
  public void onGuildLeave(GuildLeaveEvent event) {
    // do stuff
  }
}
