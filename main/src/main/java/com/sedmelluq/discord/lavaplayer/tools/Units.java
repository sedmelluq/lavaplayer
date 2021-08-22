package com.sedmelluq.discord.lavaplayer.tools;

public class Units {
    /**
     * Not a negative number, so that we would not need to test for it separately when comparing.
     */
    public static final long CONTENT_LENGTH_UNKNOWN = Long.MAX_VALUE;
    public static final long DURATION_MS_UNKNOWN = Long.MAX_VALUE;
    public static final long DURATION_SEC_UNKNOWN = Long.MAX_VALUE;

    public static final long BITRATE_UNKNOWN = -1;

    private static final long SECONDS_MAXIMUM = DURATION_SEC_UNKNOWN / 1000;

    public static long secondsToMillis(long seconds) {
        if (seconds == DURATION_SEC_UNKNOWN) {
            return DURATION_MS_UNKNOWN;
        } else if (seconds > SECONDS_MAXIMUM) {
            throw new RuntimeException("Cannot convert " + seconds + " to millis - would overflow.");
        } else {
            return seconds * 1000;
        }
    }
}
