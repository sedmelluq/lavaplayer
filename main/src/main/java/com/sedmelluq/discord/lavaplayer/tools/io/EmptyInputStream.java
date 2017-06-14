package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.InputStream;

public class EmptyInputStream extends InputStream {
  public static final EmptyInputStream INSTANCE = new EmptyInputStream();

  public int available() {
    return 0;
  }

  public int read() {
    return -1;
  }
}
