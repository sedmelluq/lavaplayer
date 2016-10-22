package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.filter.FilterChainBuilder;
import com.sedmelluq.discord.lavaplayer.filter.ShortPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder.HEADER_SIZE;
import static com.sedmelluq.discord.lavaplayer.natives.mp3.Mp3Decoder.SAMPLES_PER_FRAME;

/**
 * Handles parsing MP3 files, seeking and sending the decoded frames to the specified frame consumer.
 */
public class Mp3StreamingFile {
  private static final int HEADER_NOT_READ = 0;

  private static final byte[] IDV3_TAG = new byte[] { 0x49, 0x44, 0x33 };

  private final AudioProcessingContext context;
  private final SeekableInputStream inputStream;
  private final DataInputStream dataInput;
  private final Mp3Decoder mp3Decoder;
  private final ShortBuffer outputBuffer;
  private final ByteBuffer inputBuffer;
  private final byte[] frameBuffer;
  private final byte[] scanBuffer;

  private int frameBufferPosition;
  private int frameSize;

  private int sampleRate;
  private ShortPcmAudioFilter downstream;
  private Mp3Seeker seeker;

  /**
   * @param context Configuration and output information for processing. May be null in case no frames are read and this
   *                instance is only used to retrieve information about the track.
   * @param inputStream Stream to read the file from
   */
  public Mp3StreamingFile(AudioProcessingContext context, SeekableInputStream inputStream) {
    this.context = context;
    this.inputStream = inputStream;
    this.dataInput = new DataInputStream(inputStream);
    this.outputBuffer = ByteBuffer.allocateDirect((int) SAMPLES_PER_FRAME * 4).order(ByteOrder.nativeOrder()).asShortBuffer();
    this.inputBuffer = ByteBuffer.allocateDirect(Mp3Decoder.getMaximumFrameSize());
    this.frameBuffer = new byte[Mp3Decoder.getMaximumFrameSize()];
    this.mp3Decoder = new Mp3Decoder();
    this.scanBuffer = new byte[16];
  }

  /**
   * Parses file headers to find the first MP3 frame and to get the settings for initialising the filter chain.
   * @throws IOException On read error
   */
  public void parseHeaders() throws IOException {
    boolean headerInBuffer = !skipIdv3Tags();

    if (!scanForFrame(headerInBuffer, 2048)) {
      throw new IllegalStateException("File ended before the first frame was found.");
    }

    sampleRate = Mp3Decoder.getFrameSampleRate(frameBuffer, 0);
    downstream = context != null ? FilterChainBuilder.forShortPcm(context, 2, sampleRate, true) : null;

    initialiseSeeker();
  }

  private void initialiseSeeker() throws IOException {
    long startPosition = inputStream.getPosition() - frameBufferPosition;
    dataInput.readFully(frameBuffer, frameBufferPosition, frameSize - frameBufferPosition);
    frameBufferPosition = frameSize;

    seeker = Mp3XingSeeker.createFromFrame(startPosition, inputStream.getContentLength(), frameBuffer);
    if (seeker == null) {
      seeker = Mp3ConstantRateSeeker.createFromFrame(startPosition, inputStream.getContentLength(), frameBuffer);
    }
  }

  /**
   * Decodes audio frames and sends them to frame consumer
   * @throws InterruptedException
   */
  public void provideFrames() throws InterruptedException {
    try {
      while (true) {
        if (frameSize == HEADER_NOT_READ && !scanForFrame(false, Integer.MAX_VALUE)) {
          break;
        }

        dataInput.readFully(frameBuffer, frameBufferPosition, frameSize - frameBufferPosition);

        inputBuffer.clear();
        inputBuffer.put(frameBuffer, 0, frameSize);
        inputBuffer.flip();

        int produced = mp3Decoder.decode(inputBuffer, outputBuffer);

        if (produced > 0) {
          downstream.process(outputBuffer);
        }

        frameSize = HEADER_NOT_READ;
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
      long frameIndex = seeker.seekAndGetFrameIndex(timecode, inputStream);
      long actualTimecode = frameIndex * SAMPLES_PER_FRAME * 1000 / sampleRate;
      downstream.seekPerformed(timecode, actualTimecode);

      frameBufferPosition = 0;
      frameSize = HEADER_NOT_READ;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return An estimated duration of the file in milliseconds
   */
  public long getDuration() {
    return seeker.getDuration();
  }

  /**
   * Closes resources.
   */
  public void close() {
    if (downstream != null) {
      downstream.close();
    }

    mp3Decoder.close();
  }

  private boolean scanForFrame(boolean headerInBuffer, int bytesToCheck) throws IOException {
    int bytesInBuffer = headerInBuffer ? HEADER_SIZE : 0;

    if (parseFrameAt(bytesInBuffer)) {
      return true;
    }

    return runFrameScanLoop(bytesToCheck - bytesInBuffer, bytesInBuffer);
  }

  private boolean runFrameScanLoop(int bytesToCheck, int bytesInBuffer) throws IOException {
    while (bytesToCheck > 0) {
      for (int i = bytesInBuffer; i < scanBuffer.length && bytesToCheck > 0; i++) {
        int next = inputStream.read();
        if (next == -1) {
          return false;
        }

        scanBuffer[i] = (byte) (next & 0xFF);
        bytesToCheck--;

        if (parseFrameAt(i + 1)) {
          return true;
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
    if (scanOffset >= HEADER_SIZE && (frameSize = Mp3Decoder.getFrameSize(scanBuffer, scanOffset - HEADER_SIZE)) > 0) {
      for (int i = 0; i < HEADER_SIZE; i++) {
        frameBuffer[i] = scanBuffer[scanOffset - HEADER_SIZE + i];
      }

      frameBufferPosition = HEADER_SIZE;
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
}
