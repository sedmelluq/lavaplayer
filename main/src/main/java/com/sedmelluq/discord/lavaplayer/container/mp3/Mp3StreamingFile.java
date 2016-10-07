package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.filter.FilterChainBuilder;
import com.sedmelluq.discord.lavaplayer.filter.ShortPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder.HEADER_SIZE;
import static com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder.SAMPLES_PER_FRAME;

/**
 * Handles parsing MP3 files, seeking and sending the decoded frames to the specified frame consumer.
 */
public class Mp3StreamingFile {
  private static final byte[] IDV3_TAG = new byte[] { 0x49, 0x44, 0x33 };

  private final AudioConfiguration configuration;
  private final SeekableInputStream inputStream;
  private final DataInputStream dataInput;
  private final AudioFrameConsumer frameConsumer;
  private final AtomicInteger volumeLevel;
  private final Mp3Decoder mp3Decoder;
  private final ShortBuffer outputBuffer;
  private final ByteBuffer inputBuffer;
  private final byte[] inputBufferBytes;
  private final byte[] scanBuffer;

  private Configuration track;
  private int nextFrameSize;

  /**
   * @param configuration Audio configuration to use with this track
   * @param inputStream Stream to read the file from
   * @param frameConsumer The frame consumer where the audio frames are sent
   * @param volumeLevel Mutable audio level
   */
  public Mp3StreamingFile(AudioConfiguration configuration, SeekableInputStream inputStream, AudioFrameConsumer frameConsumer, AtomicInteger volumeLevel) {
    this.configuration = configuration;
    this.inputStream = inputStream;
    this.dataInput = new DataInputStream(inputStream);
    this.frameConsumer = frameConsumer;
    this.volumeLevel = volumeLevel;
    this.outputBuffer = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME * 4).order(ByteOrder.nativeOrder()).asShortBuffer();
    this.inputBuffer = ByteBuffer.allocateDirect(Mp3Decoder.getMaximumFrameSize());
    this.inputBufferBytes = new byte[Mp3Decoder.getMaximumFrameSize()];
    this.mp3Decoder = new Mp3Decoder();
    this.scanBuffer = new byte[16];
  }

  /**
   * Parses file headers to find the first MP3 frame and to get the settings for initialising the filter chain.
   * @throws IOException On read error
   */
  public void parseHeaders() throws IOException {
    boolean headerInBuffer = !skipIdv3Tags();
    int scanOffset = scanForFrame(headerInBuffer, 2048);
    int sampleRate = Mp3Decoder.getFrameSampleRate(scanBuffer, scanOffset);

    track = new Configuration(
        inputStream.getPosition() - 4,
        sampleRate,
        FilterChainBuilder.forShortPcm(configuration, frameConsumer, volumeLevel, 2, sampleRate, true),
        Mp3Decoder.getAverageFrameSize(scanBuffer, scanOffset)
    );
  }

  /**
   * Decodes audio frames and sends them to frame consumer
   * @throws InterruptedException
   */
  public void provideFrames() throws InterruptedException {
    try {
      while (true) {
        if (nextFrameSize == 0 && scanForFrame(false, 4) < 0) {
          break;
        }

        dataInput.readFully(inputBufferBytes, 0, nextFrameSize - 4);
        inputBuffer.put(inputBufferBytes, 0, nextFrameSize - 4);
        inputBuffer.flip();

        int produced = mp3Decoder.decode(inputBuffer, outputBuffer);

        if (produced > 0) {
          track.downstream.process(outputBuffer);
        }

        nextFrameSize = 0;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Seeks to the specified timecode.
   * @param timecode The timecode in milliseconds
   */
  public void seekToTimecode(long timecode) {
    try {
      long maximumFrameCount = (long) ((inputStream.getContentLength() - track.startPosition + 8) / track.averageFrameSize);

      long sampleIndex = timecode * track.sampleRate / 1000;
      long frameIndex = Math.min(sampleIndex / SAMPLES_PER_FRAME, maximumFrameCount);

      long seekPosition = (long) (frameIndex * track.averageFrameSize) - 8;
      inputStream.seek(track.startPosition + seekPosition);
      scanForFrame(false, 16);

      long actualTimecode = frameIndex * SAMPLES_PER_FRAME * 1000 / track.sampleRate;
      track.downstream.seekPerformed(timecode, actualTimecode);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Closes resources.
   */
  public void close() {
    track.downstream.close();
    mp3Decoder.close();
  }

  private int scanForFrame(boolean headerInBuffer, int bytesToCheck) throws IOException {
    int bytesInBuffer = headerInBuffer ? HEADER_SIZE : 0;

    if (parseFrameAt(bytesInBuffer)) {
      return bytesInBuffer - HEADER_SIZE;
    }

    return runFrameScanLoop(bytesToCheck - bytesInBuffer, bytesInBuffer);
  }

  private int runFrameScanLoop(int bytesToCheck, int bytesInBuffer) throws IOException {
    while (bytesToCheck > 0) {
      for (int i = bytesInBuffer; i < scanBuffer.length && bytesToCheck > 0; i++) {
        int next = inputStream.read();
        if (next == -1) {
          return -1;
        }

        scanBuffer[i] = (byte) (next & 0xFF);
        bytesToCheck--;

        if (parseFrameAt(i + 1)) {
          return i + 1 - HEADER_SIZE;
        }
      }

      bytesInBuffer = copyScanBufferEndToBeginning();
    }

    throw new IllegalStateException("Mp3 frame not found.");
  }

  private int copyScanBufferEndToBeginning() {
    for (int i = 0; i < HEADER_SIZE - 1; i++) {
      scanBuffer[i] = scanBuffer[scanBuffer.length - HEADER_SIZE + i + 1];
    }

    return HEADER_SIZE - 1;
  }

  private boolean parseFrameAt(int scanOffset) {
    if (scanOffset >= HEADER_SIZE && (nextFrameSize = Mp3Decoder.getFrameSize(scanBuffer, scanOffset - HEADER_SIZE)) > 0) {
      inputBuffer.clear();
      inputBuffer.put(scanBuffer, scanOffset - HEADER_SIZE, HEADER_SIZE);
      return true;
    }

    return false;
  }

  private boolean skipIdv3Tags() throws IOException {
    dataInput.readFully(scanBuffer, 0, 4);

    for (int i = 0; i < 3; i++) {
      if (scanBuffer[i] != IDV3_TAG[i]) {
        return false;
      }
    }

    if (scanBuffer[3] != 3 && scanBuffer[3] != 4) {
      return false;
    }

    dataInput.readShort();

    int tagsSize = (dataInput.readByte() & 0xFF) << 21
        | (dataInput.readByte() & 0xFF) << 14
        | (dataInput.readByte() & 0xFF) << 7
        | (dataInput.readByte() & 0xFF);

    inputStream.seek(inputStream.getPosition() + tagsSize);
    return true;
  }

  private static class Configuration {
    private final long startPosition;
    private final int sampleRate;
    private final ShortPcmAudioFilter downstream;
    private final double averageFrameSize;

    private Configuration(long startPosition, int sampleRate, ShortPcmAudioFilter downstream, double averageFrameSize) {
      this.startPosition = startPosition;
      this.sampleRate = sampleRate;
      this.downstream = downstream;
      this.averageFrameSize = averageFrameSize;
    }
  }
}
