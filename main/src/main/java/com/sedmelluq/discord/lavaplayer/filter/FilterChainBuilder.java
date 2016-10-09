package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

/**
 * Builds audio filter chains based on the input format.
 */
public class FilterChainBuilder {
  /**
   * @param context Configuration and output information for processing
   * @param channels Number of channels in the input data
   * @param frequency Frequency of the input data
   * @param noPartialFrames Whether incoming buffers will always contain full frames (length % channelCount == 0)
   * @return Filter which accepts short PCM buffers
   */
  public static ShortPcmAudioFilter forShortPcm(AudioProcessingContext context, int channels, int frequency, boolean noPartialFrames) {
    OpusEncodingPcmAudioFilter opusEncoder = new OpusEncodingPcmAudioFilter(context);
    ShortPcmAudioFilter filter;

    int outChannels = OpusEncodingPcmAudioFilter.CHANNEL_COUNT;

    if (frequency != OpusEncodingPcmAudioFilter.FREQUENCY) {
      filter = new ShortToFloatPcmAudioFilter(outChannels, new ResamplingPcmAudioFilter(context.configuration, outChannels,
          opusEncoder, frequency, OpusEncodingPcmAudioFilter.FREQUENCY));
    } else {
      filter = opusEncoder;
    }

    if (channels != outChannels || !noPartialFrames) {
      filter = new ChannelCountPcmAudioFilter(channels, outChannels, filter);
    }

    return filter;
  }

  /**
   * @param context Configuration and output information for processing
   * @param channels Number of channels in the input data
   * @param frequency Frequency of the input data
   * @return Filter which accepts float PCM buffers
   */
  public static FloatPcmAudioFilter forFloatPcm(AudioProcessingContext context, int channels, int frequency) {
    FloatPcmAudioFilter filter = new OpusEncodingPcmAudioFilter(context);

    if (frequency != OpusEncodingPcmAudioFilter.FREQUENCY) {
      filter = new ResamplingPcmAudioFilter(context.configuration, channels, filter, frequency, OpusEncodingPcmAudioFilter.FREQUENCY);
    }

    return filter;
  }
}
