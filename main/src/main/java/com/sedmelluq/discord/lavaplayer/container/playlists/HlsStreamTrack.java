package com.sedmelluq.discord.lavaplayer.container.playlists;

import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.source.stream.MpegTsM3uStreamAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class HlsStreamTrack extends MpegTsM3uStreamAudioTrack {
    private final HlsStreamSegmentUrlProvider segmentUrlProvider;
    private final HttpInterfaceManager httpInterfaceManager;

    /**
     * @param trackInfo            Track info
     * @param httpInterfaceManager
     */
    public HlsStreamTrack(AudioTrackInfo trackInfo, String streamUrl, HttpInterfaceManager httpInterfaceManager,
                          boolean isInnerUrl) {

        super(trackInfo);

        segmentUrlProvider = isInnerUrl ?
            new HlsStreamSegmentUrlProvider(null, streamUrl) :
            new HlsStreamSegmentUrlProvider(streamUrl, null);

        this.httpInterfaceManager = httpInterfaceManager;
    }

    @Override
    protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
        return segmentUrlProvider;
    }

    @Override
    protected HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }
}
