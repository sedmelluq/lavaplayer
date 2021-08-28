package lavaplayer.container.playlists;

import lavaplayer.container.MediaContainerDetectionResult;
import lavaplayer.container.MediaContainerHints;
import lavaplayer.container.MediaContainerProbe;
import lavaplayer.tools.DataFormatTools;
import lavaplayer.tools.io.SeekableInputStream;
import lavaplayer.track.AudioReference;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lavaplayer.container.MediaContainerDetection.STREAM_SCAN_DISTANCE;
import static lavaplayer.container.MediaContainerDetection.matchNextBytesAsRegex;
import static lavaplayer.container.MediaContainerDetectionResult.refer;
import static lavaplayer.container.MediaContainerDetectionResult.unsupportedFormat;

/**
 * Probe for a playlist containing the raw link without any format.
 */
public class PlainPlaylistContainerProbe implements MediaContainerProbe {
    private static final Logger log = LoggerFactory.getLogger(PlainPlaylistContainerProbe.class);

    private static final Pattern linkPattern = Pattern.compile("^(?:https?|icy)://.*");

    @Override
    public String getName() {
        return "plain";
    }

    @Override
    public boolean matchesHints(MediaContainerHints hints) {
        return false;
    }

    @Override
    public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
        if (!matchNextBytesAsRegex(inputStream, STREAM_SCAN_DISTANCE, linkPattern, StandardCharsets.UTF_8)) {
            return null;
        }

        log.debug("Track {} is a plain playlist file.", reference.identifier);
        return loadFromLines(DataFormatTools.streamToLines(inputStream, StandardCharsets.UTF_8));
    }

    private MediaContainerDetectionResult loadFromLines(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = linkPattern.matcher(line);

            if (matcher.matches()) {
                return refer(this, new AudioReference(matcher.group(0), null));
            }
        }

        return unsupportedFormat(this, "The playlist file contains no links.");
    }

    @Override
    public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        throw new UnsupportedOperationException();
    }
}
