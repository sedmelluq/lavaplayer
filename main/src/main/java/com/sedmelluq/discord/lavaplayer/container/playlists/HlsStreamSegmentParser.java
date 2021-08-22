package com.sedmelluq.discord.lavaplayer.container.playlists;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.fetchResponseLines;

public class HlsStreamSegmentParser {
    public static List<HlsStreamSegment> parseFromUrl(HttpInterface httpInterface, String url) throws IOException {
        return parseFromLines(fetchResponseLines(httpInterface, new HttpGet(url), "stream segments list"));
    }

    public static List<HlsStreamSegment> parseFromLines(String[] lines) {
        List<HlsStreamSegment> segments = new ArrayList<>();
        ExtendedM3uParser.Line segmentInfo = null;

        for (String lineText : lines) {
            ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

            if (line.isDirective() && "EXTINF".equals(line.directiveName)) {
                segmentInfo = line;
            }

            if (line.isData()) {
                if (segmentInfo != null && segmentInfo.extraData.contains(",")) {
                    String[] fields = segmentInfo.extraData.split(",", 2);
                    segments.add(new HlsStreamSegment(line.lineData, parseSecondDuration(fields[0]), fields[1]));
                } else {
                    segments.add(new HlsStreamSegment(line.lineData, null, null));
                }
            }
        }

        return segments;
    }

    private static Long parseSecondDuration(String value) {
        try {
            double asDouble = Double.parseDouble(value);
            return (long) (asDouble * 1000.0);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
