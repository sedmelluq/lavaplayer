package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.track.playback.AllocatingAudioFrameBuffer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory;

/**
 * Configuration for audio processing.
 */
public class AudioConfiguration {
  public static final int OPUS_QUALITY_MAX = 10;

  private volatile ResamplingQuality resamplingQuality;
  private volatile int opusEncodingQuality;
  private volatile AudioDataFormat outputFormat;
  private volatile boolean filterHotSwapEnabled;
  private volatile AudioFrameBufferFactory frameBufferFactory;

  /**
   * Create a new configuration with default values.
   */
  public AudioConfiguration() {
    resamplingQuality = ResamplingQuality.LOW;
    opusEncodingQuality = OPUS_QUALITY_MAX;
    outputFormat = StandardAudioDataFormats.DISCORD_OPUS;
    filterHotSwapEnabled = false;
    frameBufferFactory = AllocatingAudioFrameBuffer::new;
  }

  public ResamplingQuality getResamplingQuality() {
    return resamplingQuality;
  }

  public void setResamplingQuality(ResamplingQuality resamplingQuality) {
    this.resamplingQuality = resamplingQuality;
  }

  public int getOpusEncodingQuality() {
    return opusEncodingQuality;
  }

  public void setOpusEncodingQuality(int opusEncodingQuality) {
    this.opusEncodingQuality = Math.max(0, Math.min(opusEncodingQuality, OPUS_QUALITY_MAX));
  }

  public AudioDataFormat getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(AudioDataFormat outputFormat) {
    this.outputFormat = outputFormat;
  }

  public boolean isFilterHotSwapEnabled() {
    return filterHotSwapEnabled;
  }

  public void setFilterHotSwapEnabled(boolean filterHotSwapEnabled) {
    this.filterHotSwapEnabled = filterHotSwapEnabled;
  }

  public AudioFrameBufferFactory getFrameBufferFactory() {
    return frameBufferFactory;
  }

  public void setFrameBufferFactory(AudioFrameBufferFactory frameBufferFactory) {
    this.frameBufferFactory = frameBufferFactory;
  }

  /**
   * @return A copy of this configuration.
   */
  public AudioConfiguration copy() {
    AudioConfiguration copy = new AudioConfiguration();
    copy.setResamplingQuality(resamplingQuality);
    copy.setOpusEncodingQuality(opusEncodingQuality);
    copy.setOutputFormat(outputFormat);
    copy.setFilterHotSwapEnabled(filterHotSwapEnabled);
    copy.setFrameBufferFactory(frameBufferFactory);
    return copy;
  }

  /**
   * Resampling quality levels
   */
  public enum ResamplingQuality {
    HIGH,
    MEDIUM,
    LOW
  }
}
