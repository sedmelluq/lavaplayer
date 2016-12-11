package com.sedmelluq.discord.lavaplayer.demo;

import net.dv8tion.jda.core.entities.Message;

import java.util.function.Consumer;

public interface MessageDispatcher {
  void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure);

  void sendMessage(String message);
}
