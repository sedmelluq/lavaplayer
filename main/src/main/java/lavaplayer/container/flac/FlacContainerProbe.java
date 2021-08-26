package lavaplayer.container.flac;

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

/**
 * Container detection probe for MP3 format.
 */
public class FlacContainerProbe implements MediaContainerProbe {
    private static final Logger log = LoggerFactory.getLogger(FlacContainerProbe.class);

    private static final String TITLE_TAG = "TITLE";
    private static final String ARTIST_TAG = "ARTIST";

    @Override
    public String getName() {
        return "flac";
    }

    @Override
    public boolean matchesHints(MediaContainerHints hints) {
        return false;
    }

    @Override
    public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
        if (!checkNextBytes(inputStream, FlacFileLoader.FLAC_CC)) {
            return null;
        }

        log.debug("Track {} is a FLAC file.", reference.getIdentifier());

        FlacTrackInfo fileInfo = new FlacFileLoader(inputStream).parseHeaders();
        AudioTrackInfo trackInfo = AudioTrackInfoBuilder.create(reference, inputStream, builder -> {
            builder.setTitle(fileInfo.tags.get(TITLE_TAG));
            builder.setAuthor(fileInfo.tags.get(ARTIST_TAG));
            builder.setLength(fileInfo.duration);
            return null;
        }).build();

        return supportedFormat(this, null, trackInfo);
    }

    @Override
    public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        return new FlacAudioTrack(trackInfo, inputStream);
    }
}
