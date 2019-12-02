package com.sedmelluq.lavaplayer.core.player.filter;

import com.sedmelluq.lavaplayer.core.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.filter.volume.VolumePostProcessor;
import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
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
  public static boolean isProcessingRequired(AudioPlaybackContext context, AudioDataFormat inputFormat) {
    AudioConfiguration configuration = context.getConfiguration();

    return !configuration.getOutputFormat().equals(inputFormat) ||
        configuration.getVolumeLevel() != 100 ||
        configuration.getFilterFactory() != null;
  }

  /**
   * Creates an audio pipeline instance based on provided settings.
   *
   * @param context Configuration and output information for processing
   * @param inputFormat The parameters of the PCM input.
   * @return A pipeline which delivers the input to the final frame destination.
   */
  public static AudioPipeline create(AudioPlaybackContext context, PcmFormat inputFormat) {
    AudioConfiguration configuration = context.getConfiguration();

    int inputChannels = inputFormat.channelCount;
    int outputChannels = configuration.getOutputFormat().channelCount;

    UniversalPcmAudioFilter end = new FinalPcmAudioFilter(context, createPostProcessors(context));
    FilterChainBuilder builder = new FilterChainBuilder();
    builder.addFirst(end);

    if (configuration.isFilterHotSwapEnabled() || configuration.getFilterFactory() != null) {
      UserProvidedAudioFilters userFilters = new UserProvidedAudioFilters(context, end);
      builder.addFirst(userFilters);
    }

    if (inputFormat.sampleRate != configuration.getOutputFormat().sampleRate) {
      builder.addFirst(new ResamplingPcmAudioFilter(configuration, outputChannels,
          builder.makeFirstFloat(outputChannels), inputFormat.sampleRate, configuration.getOutputFormat().sampleRate));
    }

    if (inputChannels != outputChannels) {
      builder.addFirst(new ChannelCountPcmAudioFilter(inputChannels, outputChannels,
          builder.makeFirstUniversal(outputChannels)));
    }

    return new AudioPipeline(builder.build(null, inputChannels));
  }

  private static Collection<AudioPostProcessor> createPostProcessors(AudioPlaybackContext context) {
    AudioConfiguration configuration = context.getConfiguration();
    AudioChunkEncoder chunkEncoder = configuration.getOutputFormat().createEncoder(configuration);

    return Arrays.asList(
        new VolumePostProcessor(context),
        new BufferingPostProcessor(context, chunkEncoder)
    );
  }
}
