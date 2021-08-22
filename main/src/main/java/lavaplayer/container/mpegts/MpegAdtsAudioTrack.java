package lavaplayer.container.mpegts;

import lavaplayer.container.adts.AdtsAudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.DelegatedAudioTrack;
import lavaplayer.track.playback.LocalAudioTrackExecutor;

import java.io.InputStream;

import static lavaplayer.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;

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
