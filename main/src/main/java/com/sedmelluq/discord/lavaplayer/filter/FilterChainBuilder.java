package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.filter.volume.VolumePostProcessor;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.OpusChunkEncoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.PcmChunkEncoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * Builds audio filter chains based on the input format.
 */
public class FilterChainBuilder {
  /**
   * @param context Configuration and output information for processing
   * @param channels Number of channels in the input data
   * @param sampleRate Frequency of the input data
   * @param noPartialFrames Whether incoming buffers will always contain full frames (length % channelCount == 0)
   * @return Filter which accepts short PCM buffers
   */
  public static ShortPcmAudioFilter forShortPcm(AudioProcessingContext context, int channels, int sampleRate, boolean noPartialFrames) {
    FinalPcmAudioFilter end = new FinalPcmAudioFilter(context, createPostProcessors(context));
    ShortPcmAudioFilter filter;

    AudioDataFormat format = context.outputFormat;

    if (sampleRate != format.sampleRate) {
      filter = new ShortToFloatPcmAudioFilter(format.channelCount, new ResamplingPcmAudioFilter(context.configuration,
          format.channelCount, end, sampleRate, format.sampleRate));
    } else {
      filter = end;
    }

    if (channels != format.channelCount || !noPartialFrames) {
      filter = new ChannelCountPcmAudioFilter(channels, format.channelCount, filter);
    }

    return filter;
  }

  /**
   * @param context Configuration and output information for processing
   * @param frequency Frequency of the input data
   * @return Filter which accepts short PCM buffers
   */
  public static SplitShortPcmAudioFilter forSplitShortPcm(AudioProcessingContext context, int frequency) {
    FinalPcmAudioFilter opusEncoder = new FinalPcmAudioFilter(context, createPostProcessors(context));
    SplitShortPcmAudioFilter filter;

    AudioDataFormat format = context.outputFormat;

    if (frequency != format.sampleRate) {
      filter = new ShortToFloatPcmAudioFilter(format.channelCount, new ResamplingPcmAudioFilter(context.configuration,
          format.channelCount, opusEncoder, frequency, format.sampleRate));
    } else {
      filter = opusEncoder;
    }

    return filter;
  }

  /**
   * @param context Configuration and output information for processing
   * @param channels Number of channels in the input data
   * @param sampleRate Frequency of the input data
   * @return Filter which accepts float PCM buffers
   */
  public static FloatPcmAudioFilter forFloatPcm(AudioProcessingContext context, int channels, int sampleRate) {
    FloatPcmAudioFilter filter = new FinalPcmAudioFilter(context, createPostProcessors(context));

    if (sampleRate != context.outputFormat.sampleRate) {
      filter = new ResamplingPcmAudioFilter(context.configuration, channels, filter, sampleRate, context.outputFormat.sampleRate);
    }

    return filter;
  }

  /**
   * @param context Audio processing context to check output format from
   * @param inputFormat Input format of the audio
   * @return True if no audio processing is currently required with this context and input format combination.
   */
  public static boolean isProcessingRequired(AudioProcessingContext context, AudioDataFormat inputFormat) {
    return !context.outputFormat.equals(inputFormat) || context.volumeLevel.get() != 100;
  }

  private static Collection<AudioPostProcessor> createPostProcessors(AudioProcessingContext context) {
    AudioChunkEncoder chunkEncoder;

    if (context.outputFormat.codec == AudioDataFormat.Codec.OPUS) {
      chunkEncoder = new OpusChunkEncoder(context.configuration, context.outputFormat);
    } else {
      chunkEncoder = new PcmChunkEncoder(context.outputFormat);
    }

    return Arrays.asList(
        new VolumePostProcessor(context),
        new BufferingPostProcessor(context, chunkEncoder)
    );
  }
}
