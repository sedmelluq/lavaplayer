package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder.MPEG1_SAMPLES_PER_FRAME;

/**
 * Seeking support for VBR files with Xing header.
 */
public class Mp3XingSeeker implements Mp3Seeker {
  private static final Logger log = LoggerFactory.getLogger(Mp3XingSeeker.class);

  private static final int XING_OFFSET = 36;
  private static final int ALL_FLAGS = 0x7;
  private static final ByteBuffer xingTagBuffer = ByteBuffer.wrap(new byte[] { 0x58, 0x69, 0x6E, 0x67 });

  private final long firstFramePosition;
  private final long contentLength;
  private final long frameCount;
  private final long dataSize;
  private final byte[] seekMapping;
  private final long duration;

  private Mp3XingSeeker(int sampleRate, long firstFramePosition, long contentLength, long frameCount, long dataSize, byte[] seekMapping) {
    this.firstFramePosition = firstFramePosition;
    this.contentLength = contentLength;
    this.frameCount = frameCount;
    this.dataSize = dataSize;
    this.seekMapping = seekMapping;
    this.duration = frameCount * MPEG1_SAMPLES_PER_FRAME * 1000L / sampleRate;
  }

  /**
   * @param firstFramePosition Position of the first frame in the file
   * @param contentLength Total length of the file
   * @param frameBuffer Buffer of the first frame
   * @return Xing seeker, if its header is found in the first frame and has all the necessary fields
   */
  public static Mp3XingSeeker createFromFrame(long firstFramePosition, long contentLength, byte[] frameBuffer) {
    ByteBuffer frame = ByteBuffer.wrap(frameBuffer);

    if (frame.getInt(XING_OFFSET) != xingTagBuffer.getInt(0)) {
      return null;
    } else if ((frame.getInt(XING_OFFSET + 4) & ALL_FLAGS) != ALL_FLAGS) {
      log.debug("Xing tag is present, but is missing some required fields.");
      return null;
    }

    int sampleRate = Mp3Decoder.getFrameSampleRate(frameBuffer, 0);
    long frameCount = frame.getInt(XING_OFFSET + 8);
    long dataSize = frame.getInt(XING_OFFSET + 12);

    byte[] seekMapping = new byte[100];
    frame.position(XING_OFFSET + 16);
    frame.get(seekMapping);

    return new Mp3XingSeeker(sampleRate, firstFramePosition, contentLength, frameCount, dataSize, seekMapping);
  }

  @Override
  public long getDuration() {
    return duration;
  }

  @Override
  public boolean isSeekable() {
    return true;
  }

  @Override
  public long seekAndGetFrameIndex(long timecode, SeekableInputStream inputStream) throws IOException {
    int percentile = (int) (timecode * 100L / duration);
    long frameIndex = frameCount * percentile / 100L;

    long seekPosition = Math.min(firstFramePosition + dataSize * (seekMapping[percentile] & 0xFF) / 256, contentLength);
    inputStream.seek(seekPosition);

    return frameIndex;
  }
}
