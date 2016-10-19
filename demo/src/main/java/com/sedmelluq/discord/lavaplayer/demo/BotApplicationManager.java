package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.demo.controller.BotCommandMappingHandler;
import com.sedmelluq.discord.lavaplayer.demo.controller.BotController;
import com.sedmelluq.discord.lavaplayer.demo.controller.BotControllerManager;
import com.sedmelluq.discord.lavaplayer.demo.music.MusicController;
import com.sedmelluq.discord.lavaplayer.jdaudp.JdaUdpQueueingHookManager;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DaemonThreadFactory;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotApplicationManager extends ListenerAdapter {
  private final JDA jda;
  private final Map<Long, BotGuildContext> guildContexts;
  private final BotControllerManager controllerManager;
  private final AudioPlayerManager playerManager;
  private final ScheduledExecutorService executorService;

  public BotApplicationManager(JDA jda) {
    this.jda = jda;

    guildContexts = new HashMap<>();
    controllerManager = new BotControllerManager();

    controllerManager.registerController(new MusicController.Factory());

    playerManager = new AudioPlayerManager();
    //playerManager.useRemoteNodes("localhost:8080");
    playerManager.enableGcMonitoring();
    JdaUdpQueueingHookManager jdaUdpQueueingHookManager = new JdaUdpQueueingHookManager();
    playerManager.setOutputHookFactory(jdaUdpQueueingHookManager);
    jdaUdpQueueingHookManager.startDispatcher();
    playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
    playerManager.registerSourceManager(new YoutubeAudioSourceManager());
    playerManager.registerSourceManager(new LocalAudioSourceManager());
    playerManager.registerSourceManager(new SoundCloudAudioSourceManager());

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
    if (event.isPrivate()) {
      return;
    }

    BotGuildContext guildContext = getContext(event.getGuild());

    controllerManager.dispatchMessage(guildContext.controllers, "~", event.getMessage(), new BotCommandMappingHandler() {
      @Override
      public void commandNotFound(Message message, String name) {

      }

      @Override
      public void commandWrongParameterCount(Message message, String name, String usage, int given, int required) {
        event.getTextChannel().sendMessage("Wrong argument count for command");
      }

      @Override
      public void commandWrongParameterType(Message message, String name, String usage, int index, String value, Class<?> expectedType) {
        event.getTextChannel().sendMessage("Wrong argument type for command");
      }

      @Override
      public void commandRestricted(Message message, String name) {
        event.getTextChannel().sendMessage("Command not permitted");
      }

      @Override
      public void commandException(Message message, String name, Throwable throwable) {
        event.getTextChannel().sendMessage("Command threw an exception");
      }
    });
  }

  @Override
  public void onGuildLeave(GuildLeaveEvent event) {
    // do stuff
  }
}
