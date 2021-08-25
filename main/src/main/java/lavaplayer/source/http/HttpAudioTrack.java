package lavaplayer.source.http;

import lavaplayer.container.MediaContainerDescriptor;
import lavaplayer.source.ItemSourceManager;
import lavaplayer.tools.Units;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.PersistentHttpStream;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.DelegatedAudioTrack;
import lavaplayer.track.InternalAudioTrack;
import lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Audio track that handles processing HTTP addresses as audio tracks.
 */
public class HttpAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(HttpAudioTrack.class);

    private final MediaContainerDescriptor containerTrackFactory;
    private final HttpItemSourceManager sourceManager;

    /**
     * @param trackInfo             Track info
     * @param containerTrackFactory Container track factory - contains the probe with its parameters.
     * @param sourceManager         Source manager used to load this track
     */
    public HttpAudioTrack(AudioTrackInfo trackInfo, MediaContainerDescriptor containerTrackFactory,
                          HttpItemSourceManager sourceManager) {

        super(trackInfo);

        this.containerTrackFactory = containerTrackFactory;
        this.sourceManager = sourceManager;
    }

    /**
     * @return The media probe which handles creating a container-specific delegated track for this track.
     */
    public MediaContainerDescriptor getContainerTrackFactory() {
        return containerTrackFactory;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            log.debug("Starting http track from URL: {}", trackInfo.identifier);

            try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, new URI(trackInfo.identifier), Units.CONTENT_LENGTH_UNKNOWN)) {
                processDelegate((InternalAudioTrack) containerTrackFactory.createTrack(trackInfo, inputStream), localExecutor);
            }
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new HttpAudioTrack(trackInfo, containerTrackFactory, sourceManager);
    }

    @Override
    public ItemSourceManager getSourceManager() {
        return sourceManager;
    }
}