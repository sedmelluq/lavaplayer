package com.sedmelluq.discord.lavaplayer.format;

import javax.sound.sampled.AudioFormat;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

/**
 * Tools to deal with audio data formats.
 */
public class AudioDataFormatTools {

  /**
   * @param format Audio data format to convert to JDK audio format
   * @return JDK audio format for the specified format.
   */
  public static AudioFormat toAudioFormat(AudioDataFormat format) {
    if (format instanceof Pcm16AudioDataFormat) {
      return new AudioFormat(
          PCM_SIGNED,
          format.sampleRate,
          16,
          format.channelCount,
          format.channelCount * 2,
          format.sampleRate,
          format.codecName().equals(Pcm16AudioDataFormat.CODEC_NAME_BE)
      );
    } else {
      throw new IllegalStateException("Only PCM is currently supported.");
    }
  }
}
