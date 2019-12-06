package com.sedmelluq.discord.lavaplayer.demo;

import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Message;

public interface MessageDispatcher {
  void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure);

  void sendMessage(String message);
}
