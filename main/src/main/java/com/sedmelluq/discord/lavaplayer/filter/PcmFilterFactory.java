package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

/**
 * Factory for custom PCM filters.
 */
public interface PcmFilterFactory {
  /**
   * Builds a filter chain for processing a track. Note that this may be called several times during the playback of a
   * single track. All filters should send the output from the filter either to the next filter in the list, or to the
   * output filter if it is the last one in the list. Only the process and flush methods should call the next filter,
   * all other methods are called individually for each filter anyway.
   *
   * @param track The track that this chain is built for.
   * @param format The output format of the track. At the point where these filters are called, the number of channels
   *               and the sample rate already matches that of the output format.
   * @param output The filter that the last filter in this chain should send its data to.
   * @return The list of filters in the built chain. May be empty, but not <code>null</code>.
   */
  List<AudioFilter> buildChain(AudioTrack track, AudioDataFormat format, UniversalPcmAudioFilter output);
}
