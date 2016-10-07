package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds audio filter chains based on the input format.
 */
public class FilterChainBuilder {
  /**
   * @param manager Audio player manager which is used for configuration
   * @param frameConsumer The consumer of the final OPUS frames
   * @param volumeLevel Mutable volume level
   * @param channels Number of channels in the input data
   * @param frequency Frequency of the input data
   * @param noPartialFrames Whether incoming buffers will always contain full frames (length % channelCount == 0)
   * @return Filter which accepts short PCM buffers
   */
  public static ShortPcmAudioFilter forShortPcm(AudioPlayerManager manager, AudioFrameConsumer frameConsumer,
                                                AtomicInteger volumeLevel, int channels,
                                                int frequency, boolean noPartialFrames) {

    OpusEncodingPcmAudioFilter opusEncoder = new OpusEncodingPcmAudioFilter(frameConsumer, volumeLevel);
    ShortPcmAudioFilter filter;

    int outChannels = OpusEncodingPcmAudioFilter.CHANNEL_COUNT;

    if (frequency != OpusEncodingPcmAudioFilter.FREQUENCY) {
      filter = new ShortToFloatPcmAudioFilter(outChannels, new ResamplingPcmAudioFilter(manager, outChannels, opusEncoder,
          frequency, OpusEncodingPcmAudioFilter.FREQUENCY));
    } else {
      filter = opusEncoder;
    }

    if (channels != outChannels || !noPartialFrames) {
      filter = new ChannelCountPcmAudioFilter(channels, outChannels, filter);
    }

    return filter;
  }

  /**
   * @param manager Audio player manager which is used for configuration
   * @param frameConsumer The consumer of the final OPUS frames
   * @param volumeLevel Mutable volume level
   * @param channels Number of channels in the input data
   * @param frequency Frequency of the input data
   * @return Filter which accepts float PCM buffers
   */
  public static FloatPcmAudioFilter forFloatPcm(AudioPlayerManager manager, AudioFrameConsumer frameConsumer,
                                                AtomicInteger volumeLevel, int channels, int frequency) {

    FloatPcmAudioFilter filter = new OpusEncodingPcmAudioFilter(frameConsumer, volumeLevel);

    if (frequency != OpusEncodingPcmAudioFilter.FREQUENCY) {
      filter = new ResamplingPcmAudioFilter(manager, channels, filter, frequency, OpusEncodingPcmAudioFilter.FREQUENCY);
    }

    return filter;
  }
}
