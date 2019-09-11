package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.container.matroska.MatroskaAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import java.nio.charset.StandardCharsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static com.sedmelluq.discord.lavaplayer.container.Formats.MIME_AUDIO_WEBM;
import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.CHARSET;
import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.convertToMapLayout;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio track that handles processing Youtube videos as audio tracks.
 */
public class YoutubeAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(YoutubeAudioTrack.class);

  private static final String DEFAULT_SIGNATURE_KEY = "signature";

  private final YoutubeAudioSourceManager sourceManager;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public YoutubeAudioTrack(AudioTrackInfo trackInfo, YoutubeAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      FormatWithUrl format = loadBestFormatWithUrl(httpInterface);

      log.debug("Starting track from URL: {}", format.signedUrl);

      if (trackInfo.isStream) {
        processStream(localExecutor, format);
      } else {
        processStatic(localExecutor, httpInterface, format);
      }
    }
  }

  private void processStatic(LocalAudioTrackExecutor localExecutor, HttpInterface httpInterface, FormatWithUrl format) throws Exception {
    try (YoutubePersistentHttpStream stream = new YoutubePersistentHttpStream(httpInterface, format.signedUrl, format.details.getContentLength())) {
      if (format.details.getType().getMimeType().endsWith("/webm")) {
        processDelegate(new MatroskaAudioTrack(trackInfo, stream), localExecutor);
      } else {
        processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
      }
    }
  }

  private void processStream(LocalAudioTrackExecutor localExecutor, FormatWithUrl format) throws Exception {
    if (MIME_AUDIO_WEBM.equals(format.details.getType().getMimeType())) {
      throw new FriendlyException("YouTube WebM streams are currently not supported.", COMMON, null);
    } else {
      try (HttpInterface streamingInterface = sourceManager.getHttpInterface()) {
        processDelegate(new YoutubeMpegStreamAudioTrack(trackInfo, streamingInterface, format.signedUrl), localExecutor);
      }
    }
  }

  private FormatWithUrl loadBestFormatWithUrl(HttpInterface httpInterface) throws Exception {
    JsonBrowser info = getTrackInfo(httpInterface);

    String playerScript = extractPlayerScriptFromInfo(info);
    List<YoutubeTrackFormat> formats = loadTrackFormats(info, httpInterface, playerScript);
    YoutubeTrackFormat format = findBestSupportedFormat(formats);

    URI signedUrl = sourceManager.getCipherManager().getValidUrl(httpInterface, playerScript, format);

    return new FormatWithUrl(format, signedUrl);
  }

  @Override
  public AudioTrack makeClone() {
    return new YoutubeAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }

  private JsonBrowser getTrackInfo(HttpInterface httpInterface) throws Exception {
    return sourceManager.getTrackInfoFromMainPage(httpInterface, getIdentifier(), true);
  }

  private List<YoutubeTrackFormat> loadTrackFormats(JsonBrowser info, HttpInterface httpInterface, String playerScript) throws Exception {
    JsonBrowser args = info.safeGet("args");

    String adaptiveFormats = args.safeGet("adaptive_fmts").text();
    if (adaptiveFormats != null) {
      return loadTrackFormatsFromAdaptive(adaptiveFormats);
    }

    String playerResponse = args.safeGet("player_response").text();

    if (playerResponse != null) {
      JsonBrowser streamingData = JsonBrowser.parse(playerResponse)
          .safeGet("streamingData");

      if (!streamingData.isNull()) {
        List<YoutubeTrackFormat> formats = loadTrackFormatsFromStreamingData(streamingData.safeGet("formats"));
        formats.addAll(loadTrackFormatsFromStreamingData(streamingData.safeGet("adaptiveFormats")));

        if (!formats.isEmpty()) {
          return formats;
        }
      }
    }

    String dashUrl = args.safeGet("dashmpd").text();
    if (dashUrl != null) {
      return loadTrackFormatsFromDash(dashUrl, httpInterface, playerScript);
    }

    String formatStreamMap = args.safeGet("url_encoded_fmt_stream_map").text();
    if (formatStreamMap != null) {
      return loadTrackFormatsFromFormatStreamMap(formatStreamMap);
    }

    log.warn("Video {} with no detected format field, arguments are: {}", getIdentifier(), args.format());

    throw new FriendlyException("Unable to play this YouTube track.", SUSPICIOUS,
        new IllegalStateException("No adaptive formats, no dash, no stream map."));
  }

  private List<YoutubeTrackFormat> loadTrackFormatsFromAdaptive(String adaptiveFormats) throws Exception {
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

  private List<YoutubeTrackFormat> loadTrackFormatsFromFormatStreamMap(String adaptiveFormats) throws Exception {
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

  private List<YoutubeTrackFormat> loadTrackFormatsFromStreamingData(JsonBrowser formats) {
    List<YoutubeTrackFormat> tracks = new ArrayList<>();
    boolean anyFailures = false;

    if (!formats.isNull() && formats.isList()) {
      for (JsonBrowser formatJson : formats.values()) {
        String cipher = formatJson.safeGet("cipher").text();
        Map<String, String> cipherInfo = cipher != null
            ? decodeUrlEncodedItems(cipher, true)
            : Collections.emptyMap();

        try {
          JsonBrowser contentLength = formatJson.safeGet("contentLength");

          if (contentLength.isNull()) {
            log.debug("Could not find content length from streamingData format {}, skipping", formatJson.format());
            continue;
          }

          tracks.add(new YoutubeTrackFormat(
                  ContentType.parse(formatJson.safeGet("mimeType").text()),
                  formatJson.safeGet("bitrate").as(Long.class),
                  contentLength.as(Long.class),
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

  private List<YoutubeTrackFormat> loadTrackFormatsFromDash(String dashUrl, HttpInterface httpInterface, String playerScript) throws Exception {
    String resolvedDashUrl = sourceManager.getCipherManager().getValidDashUrl(httpInterface, playerScript, dashUrl);

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(resolvedDashUrl))) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Invalid status code for track info page response: " + statusCode);
      }

      Document document = Jsoup.parse(response.getEntity().getContent(), CHARSET, "", Parser.xmlParser());
      return loadTrackFormatsFromDashDocument(document);
    }
  }

  private List<YoutubeTrackFormat> loadTrackFormatsFromDashDocument(Document document) {
    List<YoutubeTrackFormat> tracks = new ArrayList<>();

    for (Element adaptation : document.select("AdaptationSet")) {
      String mimeType = adaptation.attr("mimeType");

      for (Element representation : adaptation.select("Representation")) {
        String url = representation.select("BaseURL").first().text();
        String contentLength = DataFormatTools.extractBetween(url, "/clen/", "/");
        String contentType = mimeType + "; codecs=" + representation.attr("codecs");

        if (contentLength == null) {
          log.debug("Skipping format {} because the content length is missing", contentType);
          continue;
        }

        tracks.add(new YoutubeTrackFormat(
            ContentType.parse(contentType),
            Long.parseLong(representation.attr("bandwidth")),
            Long.parseLong(contentLength),
            url,
            null,
            DEFAULT_SIGNATURE_KEY
        ));
      }
    }

    return tracks;
  }

  private static String extractPlayerScriptFromInfo(JsonBrowser info) {
    return info.get("assets").get("js").text();
  }

  private static boolean isBetterFormat(YoutubeTrackFormat format, YoutubeTrackFormat other) {
    YoutubeFormatInfo info = format.getInfo();

    if (info == null) {
      return false;
    } else if (other == null) {
      return true;
    } else if (info.ordinal() != other.getInfo().ordinal()) {
      return info.ordinal() < other.getInfo().ordinal();
    } else {
      return format.getBitrate() > other.getBitrate();
    }
  }

  private static YoutubeTrackFormat findBestSupportedFormat(List<YoutubeTrackFormat> formats) throws Exception {
    YoutubeTrackFormat bestFormat = null;

    for (YoutubeTrackFormat format : formats) {
      if (isBetterFormat(format, bestFormat)) {
        bestFormat = format;
      }
    }

    if (bestFormat == null) {
      StringJoiner joiner = new StringJoiner(", ");
      formats.forEach(format -> joiner.add(format.getType().toString()));
      throw new IllegalStateException("No supported audio streams available, available types: " + joiner.toString());
    }

    return bestFormat;
  }

  private static Map<String, String> decodeUrlEncodedItems(String input, boolean escapedSeparator) {
    if (escapedSeparator) {
      input = input.replace("\\\\u0026", "&");
    }

    return convertToMapLayout(URLEncodedUtils.parse(input, StandardCharsets.UTF_8));
  }

  private static class FormatWithUrl {
    private final YoutubeTrackFormat details;
    private final URI signedUrl;

    private FormatWithUrl(YoutubeTrackFormat details, URI signedUrl) {
      this.details = details;
      this.signedUrl = signedUrl;
    }
  }
}
