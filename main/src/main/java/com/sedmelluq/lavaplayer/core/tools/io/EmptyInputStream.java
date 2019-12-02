package com.sedmelluq.lavaplayer.core.tools.io;

import java.io.InputStream;

/**
 * Represents an empty input stream.
 */
public class EmptyInputStream extends InputStream {
  public static final EmptyInputStream INSTANCE = new EmptyInputStream();

  @Override
  public int available() {
    return 0;
  }

  @Override
  public int read() {
    return -1;
  }
}
