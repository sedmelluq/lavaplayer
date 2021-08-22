package com.sedmelluq.discord.lavaplayer.source.twitch;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.source.stream.MpegTsM3uStreamAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager.getChannelIdentifierFromUrl;

/**
 * Audio track that handles processing Twitch tracks.
 */
public class TwitchStreamAudioTrack extends MpegTsM3uStreamAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(TwitchStreamAudioTrack.class);

    private final TwitchStreamAudioSourceManager sourceManager;
    private final M3uStreamSegmentUrlProvider segmentUrlProvider;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public TwitchStreamAudioTrack(AudioTrackInfo trackInfo, TwitchStreamAudioSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
        this.segmentUrlProvider = new TwitchStreamSegmentUrlProvider(getChannelName(), sourceManager);
    }

    /**
     * @return Name of the channel of the stream.
     */
    public String getChannelName() {
        return getChannelIdentifierFromUrl(trackInfo.identifier);
    }

    @Override
    protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
        return segmentUrlProvider;
    }

    @Override
    protected HttpInterface getHttpInterface() {
        return sourceManager.getHttpInterface();
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        log.debug("Starting to play Twitch channel {}.", getChannelName());

        super.process(localExecutor);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new TwitchStreamAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
