package com.sedmelluq.discord.lavaplayer.source.stream;

import com.sedmelluq.discord.lavaplayer.container.adts.AdtsAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpegts.MpegTsElementaryInputStream;
import com.sedmelluq.discord.lavaplayer.container.mpegts.PesPacketInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

import java.io.InputStream;

import static com.sedmelluq.discord.lavaplayer.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;

public abstract class MpegTsM3uStreamAudioTrack extends M3uStreamAudioTrack {
    /**
     * @param trackInfo Track info
     */
    public MpegTsM3uStreamAudioTrack(AudioTrackInfo trackInfo) {
        super(trackInfo);
    }

    @Override
    protected void processJoinedStream(LocalAudioTrackExecutor localExecutor, InputStream stream) throws Exception {
        MpegTsElementaryInputStream elementaryInputStream = new MpegTsElementaryInputStream(stream, ADTS_ELEMENTARY_STREAM);
        PesPacketInputStream pesPacketInputStream = new PesPacketInputStream(elementaryInputStream);

        processDelegate(new AdtsAudioTrack(trackInfo, pesPacketInputStream), localExecutor);
    }
}
