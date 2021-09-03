package lavaplayer.container.wav;

import lavaplayer.container.MediaContainerDetectionResult;
import lavaplayer.container.MediaContainerHints;
import lavaplayer.container.MediaContainerProbe;
import lavaplayer.tools.io.SeekableInputStream;
import lavaplayer.track.AudioReference;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static lavaplayer.container.MediaContainerDetection.*;
import static lavaplayer.container.MediaContainerDetectionResult.supportedFormat;
import static lavaplayer.container.wav.WavFileLoader.WAV_RIFF_HEADER;
import static lavaplayer.tools.DataFormatTools.defaultOnNull;

/**
 * Container detection probe for WAV format.
 */
public class WavContainerProbe implements MediaContainerProbe {
    private static final Logger log = LoggerFactory.getLogger(WavContainerProbe.class);

    @Override
    public String getName() {
        return "wav";
    }

    @Override
    public boolean matchesHints(MediaContainerHints hints) {
        return false;
    }

    @Override
    public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
        if (!checkNextBytes(inputStream, WAV_RIFF_HEADER)) {
            return null;
        }

        log.debug("Track {} is a WAV file.", reference.getIdentifier());

        WavFileInfo fileInfo = new WavFileLoader(inputStream).parseHeaders();

        AudioTrackInfo trackInfo = new AudioTrackInfo(
            defaultOnNull(reference.getTitle(), UNKNOWN_TITLE),
            UNKNOWN_ARTIST,
            fileInfo.getDuration(),
            reference.getIdentifier(),
            false,
            reference.getIdentifier(),
            null
        );

        return supportedFormat(this, null, trackInfo);
    }

    @Override
    public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        return new WavAudioTrack(trackInfo, inputStream);
    }
}
