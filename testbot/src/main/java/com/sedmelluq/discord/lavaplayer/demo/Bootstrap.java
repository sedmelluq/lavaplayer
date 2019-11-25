package com.sedmelluq.discord.lavaplayer.demo;

import net.dv8tion.jda.api.JDABuilder;

public class Bootstrap {
  public static void main(String[] args) throws Exception {
    new JDABuilder()
        .setToken(System.getProperty("botToken"))
        .addEventListeners(new BotApplicationManager())
        .build();
  }
}
