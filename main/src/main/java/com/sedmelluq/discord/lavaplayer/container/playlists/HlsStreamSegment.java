package com.sedmelluq.discord.lavaplayer.container.playlists;

public class HlsStreamSegment {
    /**
     * URL of the segment.
     */
    public final String url;
    /**
     * Duration of the segment in milliseconds. <code>null</code> if unknown.
     */
    public final Long duration;
    /**
     * Name of the segment. <code>null</code> if unknown.
     */
    public final String name;

    public HlsStreamSegment(String url, Long duration, String name) {
        this.url = url;
        this.duration = duration;
        this.name = name;
    }
}
