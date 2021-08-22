package lavaplayer.source.soundcloud;

import lavaplayer.container.ogg.OggPacketInputStream;
import lavaplayer.container.ogg.OggTrackBlueprint;
import lavaplayer.container.ogg.OggTrackHandler;
import lavaplayer.container.ogg.OggTrackLoader;
import lavaplayer.tools.io.SeekableInputStream;
import lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;
import java.util.function.Supplier;

public class SoundCloudOpusSegmentDecoder implements SoundCloudSegmentDecoder {
    private final Supplier<SeekableInputStream> nextStreamProvider;
    private OggPacketInputStream lastJoinedStream;
    private OggTrackBlueprint blueprint;

    public SoundCloudOpusSegmentDecoder(Supplier<SeekableInputStream> nextStreamProvider) {
        this.nextStreamProvider = nextStreamProvider;
    }

    @Override
    public void prepareStream(boolean beginning) throws IOException {
        OggPacketInputStream stream = obtainStream();

        if (beginning) {
            OggTrackBlueprint newBlueprint = OggTrackLoader.loadTrackBlueprint(stream);

            if (blueprint == null) {
                if (newBlueprint == null) {
                    throw new IOException("No OGG track detected in the stream.");
                }

                blueprint = newBlueprint;
            }
        } else {
            stream.startNewTrack();
        }
    }

    @Override
    public void resetStream() throws IOException {
        if (lastJoinedStream != null) {
            lastJoinedStream.close();
            lastJoinedStream = null;
        }
    }

    @Override
    public void playStream(
        AudioProcessingContext context,
        long startPosition,
        long desiredPosition
    ) throws InterruptedException, IOException {
        try (OggTrackHandler handler = blueprint.loadTrackHandler(obtainStream())) {
            handler.initialise(
                context,
                startPosition,
                desiredPosition
            );

            handler.provideFrames();
        }
    }

    @Override
    public void close() throws Exception {
        resetStream();
    }

    private OggPacketInputStream obtainStream() {
        if (lastJoinedStream == null) {
            lastJoinedStream = new OggPacketInputStream(nextStreamProvider.get(), true);
        }

        return lastJoinedStream;
    }
}
