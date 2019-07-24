package com.sedmelluq.discord.lavaplayer.demo.d4j;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import discord4j.voice.AudioProvider;
import java.nio.ByteBuffer;

/**
 * This is a wrapper around AudioPlayer which makes it behave as an IAudioProvider for D4J. As D4J calls canProvide
 * before every call to provide(), we pull the frame in canProvide() and use the frame we already pulled in
 * provide().
 */
public class D4jAudioProvider extends AudioProvider {
  private final MutableAudioFrame frame = new MutableAudioFrame();
  private final AudioPlayer player;

  public D4jAudioProvider(AudioPlayer player) {
    super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
    this.player = player;
    this.frame.setBuffer(getBuffer());
  }

  @Override
  public boolean provide() {
    boolean didProvide = player.provide(frame);
    if (didProvide) getBuffer().flip();
    return didProvide;
  }
}