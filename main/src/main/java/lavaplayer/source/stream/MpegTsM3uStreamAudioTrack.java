package lavaplayer.source.stream;

import lavaplayer.container.adts.AdtsAudioTrack;
import lavaplayer.container.mpegts.MpegTsElementaryInputStream;
import lavaplayer.container.mpegts.PesPacketInputStream;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.playback.LocalAudioTrackExecutor;

import java.io.InputStream;

import static lavaplayer.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;

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
