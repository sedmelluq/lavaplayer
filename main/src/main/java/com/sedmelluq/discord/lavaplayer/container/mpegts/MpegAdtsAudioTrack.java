package com.sedmelluq.discord.lavaplayer.container.mpegts;

import com.sedmelluq.discord.lavaplayer.container.adts.AdtsAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

import java.io.InputStream;

import static com.sedmelluq.discord.lavaplayer.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;

public class MpegAdtsAudioTrack extends DelegatedAudioTrack {
    private final InputStream inputStream;

    /**
     * @param trackInfo Track info
     */
    public MpegAdtsAudioTrack(AudioTrackInfo trackInfo, InputStream inputStream) {
        super(trackInfo);

        this.inputStream = inputStream;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        MpegTsElementaryInputStream elementaryInputStream = new MpegTsElementaryInputStream(inputStream, ADTS_ELEMENTARY_STREAM);
        PesPacketInputStream pesPacketInputStream = new PesPacketInputStream(elementaryInputStream);
        processDelegate(new AdtsAudioTrack(trackInfo, pesPacketInputStream), executor);
    }
}
