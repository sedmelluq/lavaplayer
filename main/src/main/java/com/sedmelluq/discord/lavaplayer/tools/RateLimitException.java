package com.sedmelluq.discord.lavaplayer.tools;

import java.io.IOException;

public final class RateLimitException extends IOException {

  public RateLimitException() {
    super();
  }

  public RateLimitException(final String message) {
    super(message);
  }

  public RateLimitException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
