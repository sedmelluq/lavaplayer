package lavaplayer.source.soundcloud;

import lavaplayer.container.mp3.Mp3TrackProvider;
import lavaplayer.tools.io.SeekableInputStream;
import lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;
import java.util.function.Supplier;

public class SoundCloudMp3SegmentDecoder implements SoundCloudSegmentDecoder {
    private final Supplier<SeekableInputStream> nextStreamProvider;

    public SoundCloudMp3SegmentDecoder(Supplier<SeekableInputStream> nextStreamProvider) {
        this.nextStreamProvider = nextStreamProvider;
    }

    @Override
    public void prepareStream(boolean beginning) {
        // Nothing to do.
    }

    @Override
    public void resetStream() {
        // Nothing to do.
    }

    @Override
    public void playStream(
        AudioProcessingContext context,
        long startPosition,
        long desiredPosition
    ) throws InterruptedException, IOException {
        try (SeekableInputStream stream = nextStreamProvider.get()) {
            Mp3TrackProvider trackProvider = new Mp3TrackProvider(context, stream);

            try {
                trackProvider.parseHeaders();
                trackProvider.provideFrames();
            } finally {
                trackProvider.close();
            }
        }
    }

    @Override
    public void close() {
        // Nothing to do.
    }
}
