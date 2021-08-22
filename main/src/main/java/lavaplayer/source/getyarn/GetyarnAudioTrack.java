package lavaplayer.source.getyarn;

import lavaplayer.container.mpeg.MpegAudioTrack;
import lavaplayer.tools.Units;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.PersistentHttpStream;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.DelegatedAudioTrack;
import lavaplayer.track.playback.LocalAudioTrackExecutor;
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
