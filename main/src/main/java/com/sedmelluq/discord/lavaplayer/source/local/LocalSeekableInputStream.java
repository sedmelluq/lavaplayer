package com.sedmelluq.discord.lavaplayer.source.local;

import com.sedmelluq.discord.lavaplayer.tools.io.ExtendedBufferedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;

/**
 * Seekable input stream implementation for local files.
 */
public class LocalSeekableInputStream extends SeekableInputStream {
  private static final Logger log = LoggerFactory.getLogger(LocalSeekableInputStream.class);

  private final FileInputStream inputStream;
  private final FileChannel channel;
  private final ExtendedBufferedInputStream bufferedStream;
  private long position;

  /**
   * @param file File to create a stream for.
   */
  public LocalSeekableInputStream(File file) {
    super(file.length(), 0);

    try {
      inputStream = new FileInputStream(file);
      bufferedStream = new ExtendedBufferedInputStream(inputStream);
      channel = inputStream.getChannel();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int read() throws IOException {
    int result = bufferedStream.read();
    if (result >= 0) {
      position++;
    }

    return result;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int read = bufferedStream.read(b, off, len);
    position += read;
    return read;
  }

  @Override
  public long skip(long n) throws IOException {
    long skipped = bufferedStream.skip(n);
    position += skipped;
    return skipped;
  }

  @Override
  public int available() throws IOException {
    return bufferedStream.available();
  }

  @Override
  public synchronized void reset() throws IOException {
    throw new IOException("mark/reset not supported");
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public void close() throws IOException {
    try {
      channel.close();
    } catch (IOException e) {
      log.debug("Failed to close channel", e);
    }
  }

  @Override
  public long getPosition() {
    return position;
  }

  @Override
  public boolean canSeekHard() {
    return true;
  }

  @Override
  public List<AudioTrackInfoProvider> getTrackInfoProviders() {
    return Collections.emptyList();
  }

  @Override
  protected void seekHard(long position) throws IOException {
    channel.position(position);
    this.position = position;
    bufferedStream.discardBuffer();
  }
}
