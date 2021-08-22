package com.sedmelluq.discord.lavaplayer.source.stream;

import com.sedmelluq.discord.lavaplayer.tools.io.ChainedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

import java.io.InputStream;

/**
 * Audio track that handles processing M3U segment streams which using MPEG-TS wrapped ADTS codec.
 */
public abstract class M3uStreamAudioTrack extends DelegatedAudioTrack {
    /**
     * @param trackInfo Track info
     */
    public M3uStreamAudioTrack(AudioTrackInfo trackInfo) {
        super(trackInfo);
    }

    protected abstract M3uStreamSegmentUrlProvider getSegmentUrlProvider();

    protected abstract HttpInterface getHttpInterface();

    protected abstract void processJoinedStream(
        LocalAudioTrackExecutor localExecutor,
        InputStream stream
    ) throws Exception;

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (final HttpInterface httpInterface = getHttpInterface()) {
            try (ChainedInputStream chainedInputStream = new ChainedInputStream(() -> getSegmentUrlProvider().getNextSegmentStream(httpInterface))) {
                processJoinedStream(localExecutor, chainedInputStream);
            }
        }
    }
}
