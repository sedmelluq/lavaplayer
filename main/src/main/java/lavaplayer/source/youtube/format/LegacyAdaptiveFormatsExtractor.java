package lavaplayer.source.youtube.format;

import lavaplayer.source.youtube.YoutubeTrackFormat;
import lavaplayer.source.youtube.YoutubeTrackJsonData;
import org.apache.http.entity.ContentType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lavaplayer.tools.DataFormatTools.decodeUrlEncodedItems;

public class LegacyAdaptiveFormatsExtractor implements OfflineYoutubeTrackFormatExtractor {
    @Override
    public List<YoutubeTrackFormat> extract(YoutubeTrackJsonData data) {
        String adaptiveFormats = data.polymerArguments.get("adaptive_fmts").text();

        if (adaptiveFormats == null) {
            return Collections.emptyList();
        }

        return loadTrackFormatsFromAdaptive(adaptiveFormats);
    }

    private List<YoutubeTrackFormat> loadTrackFormatsFromAdaptive(String adaptiveFormats) {
        List<YoutubeTrackFormat> tracks = new ArrayList<>();

        for (String formatString : adaptiveFormats.split(",")) {
            Map<String, String> format = decodeUrlEncodedItems(formatString, false);

            tracks.add(new YoutubeTrackFormat(
                ContentType.parse(format.get("type")),
                Long.parseLong(format.get("bitrate")),
                Long.parseLong(format.get("clen")),
                format.get("url"),
                format.get("s"),
                format.getOrDefault("sp", DEFAULT_SIGNATURE_KEY)
            ));
        }

        return tracks;
    }
}
