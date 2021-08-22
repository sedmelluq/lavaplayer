package lavaplayer.source.youtube.format;

import lavaplayer.source.youtube.YoutubeSignatureResolver;
import lavaplayer.source.youtube.YoutubeTrackFormat;
import lavaplayer.source.youtube.YoutubeTrackJsonData;
import lavaplayer.tools.io.HttpInterface;

import java.util.List;

public interface YoutubeTrackFormatExtractor {
    String DEFAULT_SIGNATURE_KEY = "signature";

    List<YoutubeTrackFormat> extract(
        YoutubeTrackJsonData response,
        HttpInterface httpInterface,
        YoutubeSignatureResolver signatureResolver
    );
}
