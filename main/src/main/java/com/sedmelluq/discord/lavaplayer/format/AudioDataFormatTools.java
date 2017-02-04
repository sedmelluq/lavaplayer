package com.sedmelluq.discord.lavaplayer.format;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

/**
 * Tools to deal with audio data formats.
 */
public class AudioDataFormatTools {
  /**
   * Encoding object used in JDK audio format class for Opus codec.
   */
  public static final AudioFormat.Encoding OPUS_ENCODING = new AudioFormat.Encoding("OPUS");

  /**
   * @param format Audio data format to convert to JDK audio format
   * @return JDK audio format for the specified format.
   */
  public static AudioFormat toAudioFormat(AudioDataFormat format) {
    if (format.codec == AudioDataFormat.Codec.PCM_S16_BE || format.codec == AudioDataFormat.Codec.PCM_S16_LE) {
      return new AudioFormat(
          PCM_SIGNED,
          format.sampleRate,
          16,
          format.channelCount,
          format.channelCount * 2,
          format.sampleRate,
          format.codec == AudioDataFormat.Codec.PCM_S16_BE
      );
    } else if (format.codec == AudioDataFormat.Codec.OPUS) {
      return new AudioFormat(
          OPUS_ENCODING,
          format.sampleRate,
          AudioSystem.NOT_SPECIFIED,
          format.channelCount,
          AudioSystem.NOT_SPECIFIED,
          AudioSystem.NOT_SPECIFIED,
          false
      );
    } else {
      throw new IllegalStateException("All codecs should be checked.");
    }
  }
}
