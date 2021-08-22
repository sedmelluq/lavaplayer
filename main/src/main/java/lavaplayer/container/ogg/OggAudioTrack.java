package lavaplayer.container.ogg;

import lavaplayer.tools.FriendlyException;
import lavaplayer.tools.io.SeekableInputStream;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.BaseAudioTrack;
import lavaplayer.track.playback.AudioProcessingContext;
import lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio track which handles an OGG stream.
 */
public class OggAudioTrack extends BaseAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(OggAudioTrack.class);

    private final SeekableInputStream inputStream;

    /**
     * @param trackInfo   Track info
     * @param inputStream Input stream for the OGG stream
     */
    public OggAudioTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        super(trackInfo);

        this.inputStream = inputStream;
    }

    @Override
    public void process(final LocalAudioTrackExecutor localExecutor) {
        OggPacketInputStream packetInputStream = new OggPacketInputStream(inputStream, false);

        log.debug("Starting to play an OGG stream track {}", getIdentifier());

        localExecutor.executeProcessingLoop(() -> {
            try {
                processTrackLoop(packetInputStream, localExecutor.getProcessingContext());
            } catch (IOException e) {
                throw new FriendlyException("Stream broke when playing OGG track.", SUSPICIOUS, e);
            }
        }, null, true);
    }

    private void processTrackLoop(OggPacketInputStream packetInputStream, AudioProcessingContext context) throws IOException, InterruptedException {
        OggTrackBlueprint blueprint = OggTrackLoader.loadTrackBlueprint(packetInputStream);

        if (blueprint == null) {
            throw new IOException("Stream terminated before the first packet.");
        }

        while (blueprint != null) {
            try (OggTrackHandler handler = blueprint.loadTrackHandler(packetInputStream)) {
                handler.initialise(context, 0, 0);
                handler.provideFrames();
            }

            blueprint = OggTrackLoader.loadTrackBlueprint(packetInputStream);
        }
    }
}
