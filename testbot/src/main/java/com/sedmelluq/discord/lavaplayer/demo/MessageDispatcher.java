package com.sedmelluq.discord.lavaplayer.demo;

import net.dv8tion.jda.entities.Message;

public interface MessageDispatcher {
  Message sendMessage(String message);
}
