package com.sedmelluq.discord.lavaplayer.filter.equalizer;

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.Collections;
import java.util.List;

public class EqualizerFactory extends EqualizerConfiguration implements PcmFilterFactory {
  public EqualizerFactory() {
    super(new float[Equalizer.BAND_COUNT]);
  }

  @Override
  public List<AudioFilter> buildChain(AudioTrack track, AudioDataFormat format, UniversalPcmAudioFilter output) {
    if (Equalizer.isCompatible(format)) {
      return Collections.singletonList(new Equalizer(format.channelCount, output, bandMultipliers));
    } else {
      return Collections.emptyList();
    }
  }
}
