package com.sedmelluq.discord.lavaplayer.format;

import static com.sedmelluq.discord.lavaplayer.format.AudioDataFormat.Codec.OPUS;
import static com.sedmelluq.discord.lavaplayer.format.AudioDataFormat.Codec.PCM_S16_BE;

/**
 * Standard output formats compatible with Discord.
 */
public class StandardAudioDataFormats {
  /**
   * The Opus configuration used by both Discord and YouTube. Default.
   */
  public static final AudioDataFormat DISCORD_OPUS = new AudioDataFormat(2, 48000, 960, OPUS);
  /**
   * Signed 16-bit big-endian PCM format matching the parameters used by Discord.
   */
  public static final AudioDataFormat DISCORD_PCM_S16_BE = new AudioDataFormat(2, 48000, 960, PCM_S16_BE);
  /**
   * Signed 16-bit little-endian PCM format matching the parameters used by Discord.
   */
  public static final AudioDataFormat DISCORD_PCM_S16_LE = new AudioDataFormat(2, 48000, 960, PCM_S16_BE);
}
