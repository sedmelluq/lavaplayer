package com.sedmelluq.discord.lavaplayer.container.wav;

/**
 * WAV file format information.
 */
public class WavFileInfo {
  /**
   * Number of channels.
   */
  public final int channelCount;
  /**
   * Sample rate.
   */
  public final int sampleRate;
  /**
   * Bits per sample (currently only 16 supported).
   */
  public final int bitsPerSample;
  /**
   * Size of a block (one sample for each channel + padding).
   */
  public final int blockAlign;
  /**
   * Number of blocks in the file.
   */
  public final long blockCount;
  /**
   * Starting position of the raw PCM samples in the file.
   */
  public final long startOffset;

  /**
   * @param channelCount Number of channels.
   * @param sampleRate Sample rate.
   * @param bitsPerSample Bits per sample (currently only 16 supported).
   * @param blockAlign Size of a block (one sample for each channel + padding).
   * @param blockCount Number of blocks in the file.
   * @param startOffset Starting position of the raw PCM samples in the file.
   */
  public WavFileInfo(int channelCount, int sampleRate, int bitsPerSample, int blockAlign, long blockCount, long startOffset) {
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
    this.bitsPerSample = bitsPerSample;
    this.blockAlign = blockAlign;
    this.blockCount = blockCount;
    this.startOffset = startOffset;
  }

  /**
   * @return Duration of the file in milliseconds.
   */
  public long getDuration() {
    return blockCount * 1000L / sampleRate;
  }

  /**
   * @return The size of padding in a sample block in bytes.
   */
  public int getPadding() {
    return blockAlign - channelCount * (bitsPerSample >> 3);
  }
}
