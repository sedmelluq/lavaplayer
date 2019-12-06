package com.sedmelluq.discord.lavaplayer.demo.controller;

import net.dv8tion.jda.api.entities.Message;

public interface BotCommandMappingHandler {
  void commandNotFound(Message message, String name);

  void commandWrongParameterCount(Message message, String name, String usage, int given, int required);

  void commandWrongParameterType(Message message, String name, String usage, int index, String value, Class<?> expectedType);

  void commandRestricted(Message message, String name);

  void commandException(Message message, String name, Throwable throwable);
}
