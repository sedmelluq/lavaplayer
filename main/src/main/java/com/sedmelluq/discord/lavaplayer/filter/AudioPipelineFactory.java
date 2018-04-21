package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.filter.volume.VolumePostProcessor;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.OpusChunkEncoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.PcmChunkEncoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.util.Arrays;
import java.util.Collection;

public class AudioPipelineFactory {
  /**
   * @param context Audio processing context to check output format from
   * @param inputFormat Input format of the audio
   * @return True if no audio processing is currently required with this context and input format combination.
   */
  public static boolean isProcessingRequired(AudioProcessingContext context, AudioDataFormat inputFormat) {
    return !context.outputFormat.equals(inputFormat) || context.playerOptions.volumeLevel.get() != 100 ||
        context.playerOptions.filterFactory.get() != null;
  }

  public static AudioPipeline create(AudioProcessingContext context, PcmFormat inputFormat) {
    UniversalPcmAudioFilter end = new FinalPcmAudioFilter(context, createPostProcessors(context));
    FilterChainBuilder builder = new FilterChainBuilder(context.outputFormat.channelCount);
    builder.addFirst(end);

    if (context.filterHotSwapEnabled || context.playerOptions.filterFactory.get() != null) {
      UserProvidedAudioFilters userFilters = new UserProvidedAudioFilters(context, end);
      builder.addFirst(userFilters);
    }

    if (inputFormat.sampleRate != context.outputFormat.sampleRate) {
      builder.addFirst(new ResamplingPcmAudioFilter(context.configuration, context.outputFormat.channelCount,
          builder.makeFirstFloat(), inputFormat.sampleRate, context.outputFormat.sampleRate));
    }

    if (inputFormat.channelCount != context.outputFormat.channelCount) {
      builder.addFirst(new ChannelCountPcmAudioFilter(inputFormat.channelCount, context.outputFormat.channelCount,
          builder.makeFirstUniversal()));
    }

    return new AudioPipeline(builder.build(null, inputFormat.channelCount));
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
