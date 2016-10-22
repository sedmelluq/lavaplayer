package com.sedmelluq.discord.lavaplayer.container.flac;

import com.sedmelluq.discord.lavaplayer.filter.FilterChainBuilder;
import com.sedmelluq.discord.lavaplayer.filter.SplitShortPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamReader;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;

/**
 * A provider of audio frames from a FLAC track.
 */
public class FlacTrackStream {
  private final FlacTrackInfo info;
  private final SeekableInputStream inputStream;
  private final SplitShortPcmAudioFilter downstream;
  private final BitStreamReader bitStreamReader;

  /**
   * @param context Configuration and output information for processing
   * @param info Track information from FLAC metadata
   * @param inputStream Input stream to use
   */
  public FlacTrackStream(AudioProcessingContext context, FlacTrackInfo info, SeekableInputStream inputStream) {
    this.info = info;
    this.inputStream = inputStream;
    this.downstream = FilterChainBuilder.forSplitShortPcm(context, info.stream.sampleRate);
    this.bitStreamReader = new BitStreamReader(inputStream);
  }

  /**
   * Decodes audio frames and sends them to frame consumer
   * @throws InterruptedException
   */
  public void provideFrames() throws InterruptedException {
    try {
      FlacFrameInfo frameInfo = parseFrameHeader();
      if (frameInfo == null) {
        return;
      }

      processFrame(frameInfo);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private FlacFrameInfo parseFrameHeader() throws IOException {
    if (!skipToFrameSync()) {
      return null;
    }

    return FlacFrameHeaderReader.readFrameHeader(bitStreamReader, info.stream);
  }

  private boolean skipToFrameSync() throws IOException {
    int lastByte = -1;
    int currentByte;

    while ((currentByte = inputStream.read()) != -1) {
      if (lastByte == 0xFF && (currentByte & 0xFE) == 0xFE) {
        return true;
      }
      lastByte = currentByte;
    }

    return false;
  }

  private void processFrame(FlacFrameInfo frameInfo) throws IOException {
    // TODO: Finish this
  }

  /**
   * Seeks to the specified timecode.
   * @param timecode The timecode in milliseconds
   */
  public void seekToTimecode(long timecode) {
    try {
      FlacSeekPoint seekPoint = findSeekPointForTime(timecode);
      inputStream.seek(info.firstFramePosition + seekPoint.byteOffset);
      downstream.seekPerformed(timecode, seekPoint.sampleIndex * 1000 / info.stream.sampleRate);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private FlacSeekPoint findSeekPointForTime(long timecode) {
    if (info.seekPointCount == 0) {
      return new FlacSeekPoint(0, 0, 0);
    }

    long targetSampleIndex = timecode * info.stream.sampleRate / 1000L;
    return binarySearchSeekPoints(info.seekPoints, info.seekPointCount, targetSampleIndex);
  }

  private FlacSeekPoint binarySearchSeekPoints(FlacSeekPoint[] seekPoints, int length, long targetSampleIndex) {
    int low = 0;
    int high = length - 1;

    while (high > low) {
      int mid = (low + high + 1) / 2;

      if (info.seekPoints[mid].sampleIndex > targetSampleIndex) {
        high = mid - 1;
      } else {
        low = mid;
      }
    }

    return seekPoints[low];
  }

  /**
   * Free all resources associated to processing the track.
   */
  public void close() {
    downstream.close();
  }
}
