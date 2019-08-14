package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.filter.volume.VolumePostProcessor;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for audio pipelines. Contains helper methods to determine whether an audio pipeline is even required.
 */
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

  /**
   * Creates an audio pipeline instance based on provided settings.
   *
   * @param context Configuration and output information for processing
   * @param inputFormat The parameters of the PCM input.
   * @return A pipeline which delivers the input to the final frame destination.
   */
  public static AudioPipeline create(AudioProcessingContext context, PcmFormat inputFormat) {
    int inputChannels = inputFormat.channelCount;
    int outputChannels = context.outputFormat.channelCount;

    UniversalPcmAudioFilter end = new FinalPcmAudioFilter(context, createPostProcessors(context));
    FilterChainBuilder builder = new FilterChainBuilder();
    builder.addFirst(end);

    if (context.filterHotSwapEnabled || context.playerOptions.filterFactory.get() != null) {
      UserProvidedAudioFilters userFilters = new UserProvidedAudioFilters(context, end);
      builder.addFirst(userFilters);
    }

    if (inputFormat.sampleRate != context.outputFormat.sampleRate) {
      builder.addFirst(new ResamplingPcmAudioFilter(context.configuration, outputChannels,
          builder.makeFirstFloat(outputChannels), inputFormat.sampleRate, context.outputFormat.sampleRate));
    }

    if (inputChannels != outputChannels) {
      builder.addFirst(new ChannelCountPcmAudioFilter(inputChannels, outputChannels,
          builder.makeFirstUniversal(outputChannels)));
    }

    return new AudioPipeline(builder.build(null, inputChannels));
  }

  private static Collection<AudioPostProcessor> createPostProcessors(AudioProcessingContext context) {
    AudioChunkEncoder chunkEncoder = context.outputFormat.createEncoder(context.configuration);

    return Arrays.asList(
        new VolumePostProcessor(context),
        new BufferingPostProcessor(context, chunkEncoder)
    );
  }
}
