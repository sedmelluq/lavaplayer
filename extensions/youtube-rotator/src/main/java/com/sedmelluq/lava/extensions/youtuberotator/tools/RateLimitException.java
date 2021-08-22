package com.sedmelluq.lava.extensions.youtuberotator.tools;

public final class RateLimitException extends RuntimeException {

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
