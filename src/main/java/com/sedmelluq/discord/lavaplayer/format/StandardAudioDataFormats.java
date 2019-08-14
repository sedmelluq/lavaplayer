package com.sedmelluq.discord.lavaplayer.format;

/**
 * Standard output formats compatible with Discord.
 */
public class StandardAudioDataFormats {
  /**
   * The Opus configuration used by both Discord and YouTube. Default.
   */
  public static final AudioDataFormat DISCORD_OPUS = new OpusAudioDataFormat(2, 48000, 960);
  /**
   * Signed 16-bit big-endian PCM format matching the parameters used by Discord.
   */
  public static final AudioDataFormat DISCORD_PCM_S16_BE = new Pcm16AudioDataFormat(2, 48000, 960, true);
  /**
   * Signed 16-bit little-endian PCM format matching the parameters used by Discord.
   */
  public static final AudioDataFormat DISCORD_PCM_S16_LE = new Pcm16AudioDataFormat(2, 48000, 960, false);
  /**
   * Signed 16-bit big-endian PCM format matching with the most common sample rate.
   */
  public static final AudioDataFormat COMMON_PCM_S16_BE = new Pcm16AudioDataFormat(2, 44100, 960, true);
  /**
   * Signed 16-bit big-endian PCM format matching with the most common sample rate.
   */
  public static final AudioDataFormat COMMON_PCM_S16_LE = new Pcm16AudioDataFormat(2, 44100, 960, false);
}
