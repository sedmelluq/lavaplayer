package lavaplayer.source.local;

import lavaplayer.container.*;
import lavaplayer.source.ProbingItemSourceManager;
import lavaplayer.tools.FriendlyException;
import lavaplayer.track.AudioItem;
import lavaplayer.track.AudioReference;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.loading.ItemLoader;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding audio files from the local file system.
 */
public class LocalItemSourceManager extends ProbingItemSourceManager {
    public LocalItemSourceManager() {
        this(MediaContainerRegistry.DEFAULT_REGISTRY);
    }

    public LocalItemSourceManager(MediaContainerRegistry containerRegistry) {
        super(containerRegistry);
    }

    @Override
    public String getSourceName() {
        return "local";
    }

    @Override
    public AudioItem loadItem(ItemLoader itemLoader, AudioReference reference) {
        File file = new File(reference.getIdentifier());

        if (file.exists() && file.isFile() && file.canRead()) {
            return handleLoadResult(detectContainerForFile(reference, file));
        } else {
            return null;
        }
    }

    @Override
    protected AudioTrack createTrack(AudioTrackInfo trackInfo, MediaContainerDescriptor containerTrackFactory) {
        return new LocalAudioTrack(trackInfo, containerTrackFactory, this);
    }

    private MediaContainerDetectionResult detectContainerForFile(AudioReference reference, File file) {
        try (LocalSeekableInputStream inputStream = new LocalSeekableInputStream(file)) {
            int lastDotIndex = file.getName().lastIndexOf('.');
            String fileExtension = lastDotIndex >= 0 ? file.getName().substring(lastDotIndex + 1) : null;

            return new MediaContainerDetection(containerRegistry, reference, inputStream,
                MediaContainerHints.from(null, fileExtension)).detectContainer();
        } catch (IOException e) {
            throw new FriendlyException("Failed to open file for reading.", SUSPICIOUS, e);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        encodeTrackFactory(((LocalAudioTrack) track).getContainerTrackFactory(), output);
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        MediaContainerDescriptor containerTrackFactory = decodeTrackFactory(input);

        if (containerTrackFactory != null) {
            return new LocalAudioTrack(trackInfo, containerTrackFactory, this);
        }

        return null;
    }

    @Override
    public void shutdown() {
        // Nothing to shut down
    }
}
