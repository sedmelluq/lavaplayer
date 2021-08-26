package lavaplayer.container.ogg;

import lavaplayer.container.MediaContainerDetectionResult;
import lavaplayer.container.MediaContainerHints;
import lavaplayer.container.MediaContainerProbe;
import lavaplayer.tools.io.SeekableInputStream;
import lavaplayer.track.AudioReference;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.info.AudioTrackInfoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static lavaplayer.container.MediaContainerDetection.checkNextBytes;
import static lavaplayer.container.MediaContainerDetectionResult.supportedFormat;
import static lavaplayer.container.ogg.OggPacketInputStream.OGG_PAGE_HEADER;

/**
 * Container detection probe for OGG stream.
 */
public class OggContainerProbe implements MediaContainerProbe {
    private static final Logger log = LoggerFactory.getLogger(OggContainerProbe.class);

    @Override
    public String getName() {
        return "ogg";
    }

    @Override
    public boolean matchesHints(MediaContainerHints hints) {
        return false;
    }

    @Override
    public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream stream) throws IOException {
        if (!checkNextBytes(stream, OGG_PAGE_HEADER)) {
            return null;
        }

        log.debug("Track {} is an OGG stream.", reference.getIdentifier());

        AudioTrackInfoBuilder infoBuilder = AudioTrackInfoBuilder.create(reference, stream).setIsStream(true);

        try {
            collectStreamInformation(stream, infoBuilder);
        } catch (Exception e) {
            log.warn("Failed to collect additional information on OGG stream.", e);
        }

        return supportedFormat(this, null, infoBuilder.build());
    }

    @Override
    public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        return new OggAudioTrack(trackInfo, inputStream);
    }

    private void collectStreamInformation(SeekableInputStream stream, AudioTrackInfoBuilder infoBuilder) throws IOException {
        OggPacketInputStream packetInputStream = new OggPacketInputStream(stream, false);
        OggMetadata metadata = OggTrackLoader.loadMetadata(packetInputStream);

        if (metadata != null) {
            infoBuilder.apply(metadata);
        }
    }
}
