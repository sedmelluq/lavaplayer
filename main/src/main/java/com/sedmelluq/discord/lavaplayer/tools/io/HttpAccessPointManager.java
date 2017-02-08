package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.Closeable;

public interface HttpAccessPointManager extends Closeable {
  HttpAccessPoint getAccessPoint();
}
