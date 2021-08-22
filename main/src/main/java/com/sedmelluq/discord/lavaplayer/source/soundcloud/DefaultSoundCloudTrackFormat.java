package com.sedmelluq.discord.lavaplayer.source.soundcloud;

public class DefaultSoundCloudTrackFormat implements SoundCloudTrackFormat {
    private final String trackId;
    private final String protocol;
    private final String mimeType;
    private final String lookupUrl;

    public DefaultSoundCloudTrackFormat(String trackId, String protocol, String mimeType, String lookupUrl) {
        this.trackId = trackId;
        this.protocol = protocol;
        this.mimeType = mimeType;
        this.lookupUrl = lookupUrl;
    }

    @Override
    public String getTrackId() {
        return trackId;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getLookupUrl() {
        return lookupUrl;
    }
}
