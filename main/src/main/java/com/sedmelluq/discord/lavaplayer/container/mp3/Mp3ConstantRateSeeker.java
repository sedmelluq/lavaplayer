package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder.MPEG1_SAMPLES_PER_FRAME;

/**
 * MP3 seeking support for constant bitrate files or in cases where the variable bitrate format used by the file is not
 * supported. In case the file is not actually CBR, this being used as a fallback may cause inaccurate seeking.
 */
public class Mp3ConstantRateSeeker implements Mp3Seeker {
  private final double averageFrameSize;
  private final int sampleRate;
  private final long firstFramePosition;
  private final long contentLength;

  private Mp3ConstantRateSeeker(double averageFrameSize, int sampleRate, long firstFramePosition, long contentLength) {
    this.averageFrameSize = averageFrameSize;
    this.sampleRate = sampleRate;
    this.firstFramePosition = firstFramePosition;
    this.contentLength = contentLength;
  }

  /**
   * @param firstFramePosition Position of the first frame in the file
   * @param contentLength Total length of the file
   * @param frameBuffer Buffer of the first frame
   * @return Constant rate seeker, will always succeed, never null.
   */
  public static Mp3ConstantRateSeeker createFromFrame(long firstFramePosition, long contentLength, byte[] frameBuffer) {
    int sampleRate = Mp3Decoder.getFrameSampleRate(frameBuffer, 0);
    double averageFrameSize = Mp3Decoder.getAverageFrameSize(frameBuffer, 0);

    return new Mp3ConstantRateSeeker(averageFrameSize, sampleRate, firstFramePosition, contentLength);
  }

  @Override
  public long getDuration() {
    return getMaximumFrameCount() * MPEG1_SAMPLES_PER_FRAME * 1000 / sampleRate;
  }

  @Override
  public boolean isSeekable() {
    return true;
  }

  @Override
  public long seekAndGetFrameIndex(long timecode, SeekableInputStream inputStream) throws IOException {
    long maximumFrameCount = getMaximumFrameCount();

    long sampleIndex = timecode * sampleRate / 1000;
    long frameIndex = Math.min(sampleIndex / MPEG1_SAMPLES_PER_FRAME, maximumFrameCount);

    long seekPosition = (long) (frameIndex * averageFrameSize) - 8;
    inputStream.seek(firstFramePosition + seekPosition);

    return frameIndex;
  }

  private long getMaximumFrameCount() {
    return (long) ((contentLength - firstFramePosition + 8) / averageFrameSize);
  }
}
