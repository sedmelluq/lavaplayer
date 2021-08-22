package lavaplayer.source.youtube.format;

import lavaplayer.source.youtube.YoutubeSignatureResolver;
import lavaplayer.source.youtube.YoutubeTrackFormat;
import lavaplayer.source.youtube.YoutubeTrackJsonData;
import lavaplayer.tools.io.HttpInterface;

import java.util.List;

public interface OfflineYoutubeTrackFormatExtractor extends YoutubeTrackFormatExtractor {
    List<YoutubeTrackFormat> extract(YoutubeTrackJsonData data);

    @Override
    default List<YoutubeTrackFormat> extract(
        YoutubeTrackJsonData data,
        HttpInterface httpInterface,
        YoutubeSignatureResolver signatureResolver
    ) {
        return extract(data);
    }
}
