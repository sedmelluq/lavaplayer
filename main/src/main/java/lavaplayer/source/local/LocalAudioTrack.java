package lavaplayer.source.local;

import lavaplayer.container.MediaContainerDescriptor;
import lavaplayer.source.AudioSourceManager;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.DelegatedAudioTrack;
import lavaplayer.track.InternalAudioTrack;
import lavaplayer.track.playback.LocalAudioTrackExecutor;

import java.io.File;

/**
 * Audio track that handles processing local files as audio tracks.
 */
public class LocalAudioTrack extends DelegatedAudioTrack {
    private final File file;
    private final MediaContainerDescriptor containerTrackFactory;
    private final LocalAudioSourceManager sourceManager;

    /**
     * @param trackInfo             Track info
     * @param containerTrackFactory Probe track factory - contains the probe with its parameters.
     * @param sourceManager         Source manager used to load this track
     */
    public LocalAudioTrack(AudioTrackInfo trackInfo, MediaContainerDescriptor containerTrackFactory,
                           LocalAudioSourceManager sourceManager) {

        super(trackInfo);

        this.file = new File(trackInfo.identifier);
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
        try (LocalSeekableInputStream inputStream = new LocalSeekableInputStream(file)) {
            processDelegate((InternalAudioTrack) containerTrackFactory.createTrack(trackInfo, inputStream), localExecutor);
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new LocalAudioTrack(trackInfo, containerTrackFactory, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
