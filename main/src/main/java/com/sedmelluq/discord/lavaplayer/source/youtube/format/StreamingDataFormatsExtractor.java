package com.sedmelluq.discord.lavaplayer.source.youtube.format;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackFormat;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.decodeUrlEncodedItems;
import static com.sedmelluq.discord.lavaplayer.tools.Units.CONTENT_LENGTH_UNKNOWN;

public class StreamingDataFormatsExtractor implements OfflineYoutubeTrackFormatExtractor {
  private static final Logger log = LoggerFactory.getLogger(StreamingDataFormatsExtractor.class);

  @Override
  public List<YoutubeTrackFormat> extract(YoutubeTrackJsonData data) {
    JsonBrowser streamingData = data.playerResponse.get("streamingData");

    if (streamingData.isNull()) {
      return Collections.emptyList();
    }

    boolean isLive = data.playerResponse.get("videoDetails").get("isLive").asBoolean(false);

    List<YoutubeTrackFormat> formats = loadTrackFormatsFromStreamingData(streamingData.get("formats"), isLive);
    formats.addAll(loadTrackFormatsFromStreamingData(streamingData.get("adaptiveFormats"), isLive));
    return formats;
  }

  private List<YoutubeTrackFormat> loadTrackFormatsFromStreamingData(JsonBrowser formats, boolean isLive) {
    List<YoutubeTrackFormat> tracks = new ArrayList<>();
    boolean anyFailures = false;

    if (!formats.isNull() && formats.isList()) {
      for (JsonBrowser formatJson : formats.values()) {
        String cipher = formatJson.get("cipher").text();

        if (cipher == null) {
          cipher = formatJson.get("signatureCipher").text();
        }

        Map<String, String> cipherInfo = cipher != null
            ? decodeUrlEncodedItems(cipher, true)
            : Collections.emptyMap();

        try {
          long contentLength = formatJson.get("contentLength").asLong(CONTENT_LENGTH_UNKNOWN);

          if (contentLength == CONTENT_LENGTH_UNKNOWN && !isLive) {
            log.debug("Track not a live stream, but no contentLength in format {}, skipping", formatJson.format());
            continue;
          }

          tracks.add(new YoutubeTrackFormat(
              ContentType.parse(formatJson.get("mimeType").text()),
              formatJson.get("bitrate").asLong(Units.BITRATE_UNKNOWN),
              contentLength,
              cipherInfo.getOrDefault("url", formatJson.get("url").text()),
              cipherInfo.get("s"),
              cipherInfo.getOrDefault("sp", DEFAULT_SIGNATURE_KEY)
          ));
        } catch (RuntimeException e) {
          anyFailures = true;
          log.debug("Failed to parse format {}, skipping", formatJson, e);
        }
      }
    }

    if (tracks.isEmpty() && anyFailures) {
      log.warn("In streamingData adaptive formats {}, all formats either failed to load or were skipped due to missing " +
          "fields", formats.format());
    }

    return tracks;
  }
}
