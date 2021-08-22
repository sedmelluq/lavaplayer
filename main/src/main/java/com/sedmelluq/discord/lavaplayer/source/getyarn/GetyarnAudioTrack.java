package com.sedmelluq.discord.lavaplayer.source.getyarn;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class GetyarnAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(DelegatedAudioTrack.class);
    private final GetyarnAudioSourceManager sourceManager;

    public GetyarnAudioTrack(AudioTrackInfo trackInfo, GetyarnAudioSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            log.debug("Starting getyarn.io track from URL: {}", trackInfo.identifier);

            try (PersistentHttpStream inputStream = new PersistentHttpStream(
                httpInterface,
                new URI(trackInfo.identifier),
                Units.CONTENT_LENGTH_UNKNOWN
            )) {
                processDelegate(new MpegAudioTrack(trackInfo, inputStream), localExecutor);
            }
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new GetyarnAudioTrack(trackInfo, sourceManager);
    }
}
