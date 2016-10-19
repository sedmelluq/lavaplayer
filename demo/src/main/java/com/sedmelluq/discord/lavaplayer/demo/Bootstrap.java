package com.sedmelluq.discord.lavaplayer.demo;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;

import javax.security.auth.login.LoginException;

public class Bootstrap {
  public static void main(String[] args) throws InterruptedException, LoginException {
    JDA jda = new JDABuilder().setBotToken(System.getProperty("botToken")).buildBlocking();
    jda.addEventListener(new BotApplicationManager(jda));
  }
}
