package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.InputStream;

public class StreamTools {
  public static int readUntilEnd(InputStream in, byte buffer[], int offset, int length) throws IOException {
    int position = 0;

    while (position < length) {
      int count = in.read(buffer, offset + position, length - position);
      if (count < 0) {
        break;
      }

      position += count;
    }

    return position;
  }
}
