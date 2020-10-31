package com.sedmelluq.discord.lavaplayer.source.youtube.format;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackFormat;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.decodeUrlEncodedItems;

public class LegacyStreamMapFormatsExtractor implements OfflineYoutubeTrackFormatExtractor {
  private static final Logger log = LoggerFactory.getLogger(LegacyStreamMapFormatsExtractor.class);

  @Override
  public List<YoutubeTrackFormat> extract(YoutubeTrackJsonData data) {
    String formatStreamMap = data.polymerArguments.get("url_encoded_fmt_stream_map").text();

    if (formatStreamMap == null) {
      return Collections.emptyList();
    }

    return loadTrackFormatsFromFormatStreamMap(formatStreamMap);
  }

  private List<YoutubeTrackFormat> loadTrackFormatsFromFormatStreamMap(String adaptiveFormats) {
    List<YoutubeTrackFormat> tracks = new ArrayList<>();
    boolean anyFailures = false;

    for (String formatString : adaptiveFormats.split(",")) {
      try {
        Map<String, String> format = decodeUrlEncodedItems(formatString, false);
        String url = format.get("url");

        if (url == null) {
          continue;
        }

        String contentLength = DataFormatTools.extractBetween(url, "clen=", "&");

        if (contentLength == null) {
          log.debug("Could not find content length from URL {}, skipping format", url);
          continue;
        }

        tracks.add(new YoutubeTrackFormat(
            ContentType.parse(format.get("type")),
            qualityToBitrateValue(format.get("quality")),
            Long.parseLong(contentLength),
            url,
            format.get("s"),
            format.getOrDefault("sp", DEFAULT_SIGNATURE_KEY)
        ));
      } catch (RuntimeException e) {
        anyFailures = true;
        log.debug("Failed to parse format {}, skipping.", formatString, e);
      }
    }

    if (tracks.isEmpty() && anyFailures) {
      log.warn("In adaptive format map {}, all formats either failed to load or were skipped due to missing fields",
          adaptiveFormats);
    }

    return tracks;
  }

  private long qualityToBitrateValue(String quality) {
    // Return negative bitrate values to indicate missing bitrate info, but still retain the relative order.
    if ("small".equals(quality)) {
      return -10;
    } else if ("medium".equals(quality)) {
      return -5;
    } else if ("hd720".equals(quality)) {
      return -4;
    } else {
      return -1;
    }
  }
}
