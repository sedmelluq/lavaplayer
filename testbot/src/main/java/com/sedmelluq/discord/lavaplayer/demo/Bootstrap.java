package com.sedmelluq.discord.lavaplayer.demo;


import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class Bootstrap {
  public static void main(String[] args) throws Exception {
    JDA jda = new JDABuilder(AccountType.BOT)
        .setToken(System.getProperty("botToken"))
        .setAudioSendFactory(new NativeAudioSendFactory())
        .buildBlocking();

    jda.addEventListener(new BotApplicationManager(jda));
  }
}
