package com.sedmelluq.discord.lavaplayer.source.soundcloud;

public class SoundCloudM3uInfo {
    public final String lookupUrl;
    public final SoundCloudSegmentDecoder.Factory decoderFactory;

    public SoundCloudM3uInfo(String lookupUrl, SoundCloudSegmentDecoder.Factory decoderFactory) {
        this.lookupUrl = lookupUrl;
        this.decoderFactory = decoderFactory;
    }
}
