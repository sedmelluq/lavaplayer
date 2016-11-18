package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.demo.controller.BotController;

import java.util.HashMap;
import java.util.Map;

public class BotGuildContext {
  public final long guildId;
  public final Map<Class<? extends BotController>, BotController> controllers;

  public BotGuildContext(long guildId) {
    this.guildId = guildId;
    this.controllers = new HashMap<>();
  }
}
