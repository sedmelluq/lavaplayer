package com.sedmelluq.discord.lavaplayer.demo.d4j;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import sx.blah.discord.handle.audio.AudioEncodingType;
import sx.blah.discord.handle.audio.IAudioProvider;

/**
 * This is a wrapper around AudioPlayer which makes it behave as an IAudioProvider for D4J. As D4J calls canProvide
 * before every call to provide(), we pull the frame in canProvide() and use the frame we already pulled in
 * provide().
 */
public class AudioProvider implements IAudioProvider {
  private final AudioPlayer audioPlayer;
  private AudioFrame lastFrame;

  /**
   * @param audioPlayer Audio player to wrap.
   */
  public AudioProvider(AudioPlayer audioPlayer) {
    this.audioPlayer = audioPlayer;
  }

  @Override
  public boolean isReady() {
    if (lastFrame == null) {
      lastFrame = audioPlayer.provide();
    }

    return lastFrame != null;
  }

  @Override
  public byte[] provide() {
    if (lastFrame == null) {
      lastFrame = audioPlayer.provide();
    }

    byte[] data = lastFrame != null ? lastFrame.data : null;
    lastFrame = null;

    return data;
  }

  @Override
  public int getChannels() {
    return 2;
  }

  @Override
  public AudioEncodingType getAudioEncodingType() {
    return AudioEncodingType.OPUS;
  }
}
