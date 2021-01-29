package com.sedmelluq.discord.lavaplayer.source.youtube.format;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSignatureResolver;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackFormat;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyDashMpdFormatsExtractor implements YoutubeTrackFormatExtractor {
  private static final Logger log = LoggerFactory.getLogger(LegacyDashMpdFormatsExtractor.class);

  @Override
  public List<YoutubeTrackFormat> extract(
      YoutubeTrackJsonData data,
      HttpInterface httpInterface,
      YoutubeSignatureResolver signatureResolver
  ) {
    String dashUrl = data.polymerArguments.get("dashmpd").text();

    if (dashUrl == null) {
      return Collections.emptyList();
    }

    try {
      return loadTrackFormatsFromDash(dashUrl, httpInterface, signatureResolver, data.playerScriptUrl);
    } catch (Exception e) {
      throw new RuntimeException("Failed to extract formats from dash url " + dashUrl, e);
    }
  }

  private List<YoutubeTrackFormat> loadTrackFormatsFromDash(
      String dashUrl,
      HttpInterface httpInterface,
      YoutubeSignatureResolver signatureResolver,
      String playerScriptUrl
  ) throws Exception {
    String resolvedDashUrl = signatureResolver.resolveDashUrl(httpInterface, playerScriptUrl, dashUrl);

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(resolvedDashUrl))) {
      HttpClientTools.assertSuccessWithContent(response, "track info page response");

      Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "",
          Parser.xmlParser());
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
}
