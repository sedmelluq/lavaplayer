package com.sedmelluq.discord.lavaplayer.demo.controller;

import com.sedmelluq.discord.lavaplayer.demo.BotApplicationManager;
import com.sedmelluq.discord.lavaplayer.demo.BotGuildContext;
import net.dv8tion.jda.api.entities.Guild;

public interface BotControllerFactory<T extends BotController> {
  Class<T> getControllerClass();

  T create(BotApplicationManager manager, BotGuildContext state, Guild guild);
}
