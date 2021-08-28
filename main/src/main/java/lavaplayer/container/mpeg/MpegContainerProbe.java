package lavaplayer.container.mpeg;

import lavaplayer.container.MediaContainerDetectionResult;
import lavaplayer.container.MediaContainerHints;
import lavaplayer.container.MediaContainerProbe;
import lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
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
import static lavaplayer.container.MediaContainerDetectionResult.unsupportedFormat;

/**
 * Container detection probe for MP4 format.
 */
public class MpegContainerProbe implements MediaContainerProbe {
    private static final Logger log = LoggerFactory.getLogger(MpegContainerProbe.class);

    private static final int[] ISO_TAG = new int[]{0x00, 0x00, 0x00, -1, 0x66, 0x74, 0x79, 0x70};

    @Override
    public String getName() {
        return "mp4";
    }

    @Override
    public boolean matchesHints(MediaContainerHints hints) {
        return false;
    }

    @Override
    public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
        if (!checkNextBytes(inputStream, ISO_TAG)) {
            return null;
        }

        log.debug("Track {} is an MP4 file.", reference.identifier);

        MpegFileLoader file = new MpegFileLoader(inputStream);
        file.parseHeaders();

        MpegTrackInfo audioTrack = getSupportedAudioTrack(file);

        if (audioTrack == null) {
            return unsupportedFormat(this, "No supported audio format in the MP4 file.");
        }

        MpegTrackConsumer trackConsumer = new MpegNoopTrackConsumer(audioTrack);
        MpegFileTrackProvider fileReader = file.loadReader(trackConsumer);

        if (fileReader == null) {
            return unsupportedFormat(this, "MP4 file uses an unsupported format.");
        }

        AudioTrackInfo trackInfo = AudioTrackInfoBuilder.create(reference, inputStream, (builder) -> {
            builder.setTitle(file.getTextMetadata("Title"));
            builder.setAuthor(file.getTextMetadata("Artist"));
            builder.setLength(fileReader.getDuration());
            return null;
        }).build();

        return supportedFormat(this, null, trackInfo);
    }

    @Override
    public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        return new MpegAudioTrack(trackInfo, inputStream);
    }

    private MpegTrackInfo getSupportedAudioTrack(MpegFileLoader file) {
        for (MpegTrackInfo track : file.getTrackList()) {
            if ("soun".equals(track.handler) && "mp4a".equals(track.codecName)) {
                return track;
            }
        }

        return null;
    }
}
