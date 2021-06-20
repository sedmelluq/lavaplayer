package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import java.io.IOException;
import java.net.URLEncoder;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData.fromEmbedParts;
import static com.sedmelluq.discord.lavaplayer.tools.ExceptionTools.throwWithDebugInfo;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DefaultYoutubeTrackDetailsLoader implements YoutubeTrackDetailsLoader {
  private static final Logger log = LoggerFactory.getLogger(DefaultYoutubeTrackDetailsLoader.class);

  private static final String AGE_VERIFY_REQUEST_URL = "https://www.youtube.com/youtubei/v1/verify_age?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
  private static final String AGE_VERIFY_REQUEST_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20210302.07.01\"}},\"nextEndpoint\":{\"urlEndpoint\":{\"url\":\"%s\"}},\"setControvercy\":true}";
  private final HttpInterfaceManager httpInterfaceManager;

  public DefaultYoutubeTrackDetailsLoader() {
    this.httpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager();
    httpInterfaceManager.setHttpContextFilter(new BaseYoutubeHttpContextFilter());
  }

  private static final String[] EMBED_CONFIG_PREFIXES = new String[]{
      "'WEB_PLAYER_CONTEXT_CONFIGS':",
      "WEB_PLAYER_CONTEXT_CONFIGS\":",
      "'PLAYER_CONFIG':",
      "\"PLAYER_CONFIG\":"
  };

  private volatile CachedPlayerScript cachedPlayerScript = null;

  @Override
  public YoutubeTrackDetails loadDetails(HttpInterface httpInterface, String videoId, boolean requireFormats) {
    try {
      return load(httpInterface, videoId, requireFormats);
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private YoutubeTrackDetails load(
      HttpInterface httpInterface,
      String videoId,
      boolean requireFormats
  ) throws IOException {
    JsonBrowser mainInfo = loadTrackInfoFromMainPage(httpInterface, videoId);

    try {
      YoutubeTrackJsonData initialData = loadBaseResponse(mainInfo, httpInterface, videoId, requireFormats);

      if (initialData == null) {
        return null;
      }

      YoutubeTrackJsonData finalData = augmentWithPlayerScript(initialData, httpInterface, requireFormats);
      return new DefaultYoutubeTrackDetails(videoId, finalData);
    } catch (FriendlyException e) {
      throw e;
    } catch (Exception e) {
      throw throwWithDebugInfo(log, e, "Error when extracting data", "mainJson", mainInfo.format());
    }
  }

  protected YoutubeTrackJsonData loadBaseResponse(
      JsonBrowser mainInfo,
      HttpInterface httpInterface,
      String videoId,
      boolean requireFormats
  ) throws IOException {
    YoutubeTrackJsonData data = YoutubeTrackJsonData.fromMainResult(mainInfo);
    InfoStatus status = checkPlayabilityStatus(data.playerResponse);

    if (status == InfoStatus.DOES_NOT_EXIST) {
      return null;
    }

    if (status == InfoStatus.CONTENT_CHECK_REQUIRED) {
      JsonBrowser trackInfo = loadTrackInfoWithContentVerifyRequest(httpInterface, videoId);
      return YoutubeTrackJsonData.fromMainResult(trackInfo);
    }

    if (requireFormats && status == InfoStatus.REQUIRES_LOGIN) {
      JsonBrowser basicInfo = loadTrackBaseInfoFromEmbedPage(httpInterface, videoId);

      return fromEmbedParts(
          basicInfo,
          loadTrackArgsFromVideoInfoPage(videoId, basicInfo.get("sts").text())
      );
    } else {
      return data;
    }
  }

  protected InfoStatus checkPlayabilityStatus(JsonBrowser playerResponse) {
    JsonBrowser statusBlock = playerResponse.get("playabilityStatus");

    if (statusBlock.isNull()) {
      throw new RuntimeException("No playability status block.");
    }

    String status = statusBlock.get("status").text();

    if (status == null) {
      throw new RuntimeException("No playability status field.");
    } else if ("OK".equals(status)) {
      return InfoStatus.INFO_PRESENT;
    } else if ("ERROR".equals(status)) {
      String reason = statusBlock.get("reason").text();

      if ("Video unavailable".equals(reason)) {
        return InfoStatus.DOES_NOT_EXIST;
      } else {
        throw new FriendlyException(reason, COMMON, null);
      }
    } else if ("UNPLAYABLE".equals(status)) {
      String unplayableReason = getUnplayableReason(statusBlock);
      throw new FriendlyException(unplayableReason, COMMON, null);
    } else if ("LOGIN_REQUIRED".equals(status)) {
      String errorReason = statusBlock.get("errorScreen")
          .get("playerErrorMessageRenderer")
          .get("reason")
          .get("simpleText")
          .text();

      if ("Private video".equals(errorReason)) {
        throw new FriendlyException("This is a private video.", COMMON, null);
      }

      return InfoStatus.REQUIRES_LOGIN;
    } else if ("CONTENT_CHECK_REQUIRED".equals(status)) {
      return InfoStatus.CONTENT_CHECK_REQUIRED;
    } else {
      throw new FriendlyException("This video cannot be viewed anonymously.", COMMON, null);
    }
  }

  protected enum InfoStatus {
    INFO_PRESENT,
    REQUIRES_LOGIN,
    DOES_NOT_EXIST,
    CONTENT_CHECK_REQUIRED
  }

  protected String getUnplayableReason(JsonBrowser statusBlock) {
    JsonBrowser playerErrorMessage = statusBlock.get("errorScreen").get("playerErrorMessageRenderer");
    String unplayableReason = statusBlock.get("reason").text();

    if (!playerErrorMessage.get("subreason").isNull()) {
      JsonBrowser subreason = playerErrorMessage.get("subreason");

      if (!subreason.get("simpleText").isNull()) {
        unplayableReason = subreason.get("simpleText").text();
      } else if (!subreason.get("runs").isNull() && subreason.get("runs").isList()) {
        StringBuilder reasonBuilder = new StringBuilder();
        subreason.get("runs").values().forEach(
            item -> reasonBuilder.append(item.get("text").text()).append('\n')
        );
        unplayableReason = reasonBuilder.toString();
      }
    }

    return unplayableReason;
  }

  protected JsonBrowser loadTrackInfoFromMainPage(HttpInterface httpInterface, String videoId) throws IOException {
    String url = "https://www.youtube.com/watch?v=" + videoId + "&pbj=1&hl=en";

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      HttpClientTools.assertSuccessWithContent(response, "video page response");

      String responseText = EntityUtils.toString(response.getEntity(), UTF_8);

      try {
        return JsonBrowser.parse(responseText);
      } catch (FriendlyException e) {
        throw e;
      } catch (Exception e) {
        throw new FriendlyException("Received unexpected response from YouTube.", SUSPICIOUS,
            new RuntimeException("Failed to parse: " + responseText, e));
      }
    }
  }

  protected JsonBrowser loadTrackBaseInfoFromEmbedPage(HttpInterface httpInterface, String videoId) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com/embed/" + videoId))) {
      HttpClientTools.assertSuccessWithContent(response, "embed video page response");

      String html = EntityUtils.toString(response.getEntity(), UTF_8);
      String configJson = DataFormatTools.extractAfter(html, EMBED_CONFIG_PREFIXES);

      if (configJson != null) {
        // configJson is not pure JSON - it contains data after the object ends, but this does not break parsing.
        return JsonBrowser.parse(configJson);
      }

      log.debug("Did not find player config in track {} embed page HTML: {}", videoId, html);
    }

    throw new FriendlyException("Track information is unavailable.", SUSPICIOUS,
        new IllegalStateException("Expected player config is not present in embed page."));
  }

  protected JsonBrowser loadTrackArgsFromVideoInfoPage(String videoId, String sts) throws IOException {
    String videoApiUrl = "https://youtube.googleapis.com/v/" + videoId;
    String encodedApiUrl = URLEncoder.encode(videoApiUrl, UTF_8.name());
    String url = "https://www.youtube.com/get_video_info?video_id=" + videoId + "&eurl=" + encodedApiUrl +
        "&hl=en_GB&html5=1&c=ANDROID&cver=16.24";

    if (sts != null) {
      url += "&sts=" + sts;
    }

    JsonBrowser values = JsonBrowser.newMap();

    try (CloseableHttpResponse response = httpInterfaceManager.getInterface().execute(new HttpGet(url))) {
      HttpClientTools.assertSuccessWithContent(response, "video info response");

      for (NameValuePair pair : URLEncodedUtils.parse(response.getEntity())) {
        values.put(pair.getName(), pair.getValue());
      }
    }

    return values;
  }

  protected JsonBrowser loadTrackInfoWithContentVerifyRequest(HttpInterface httpInterface, String videoId) throws IOException {
    HttpPost post = new HttpPost(AGE_VERIFY_REQUEST_URL);
    StringEntity payload = new StringEntity(String.format(AGE_VERIFY_REQUEST_PAYLOAD, "/watch?v=" + videoId), "UTF-8");
    post.setEntity(payload);
    try (CloseableHttpResponse response = httpInterface.execute(post)) {
      HttpClientTools.assertSuccessWithContent(response, "content verify response");

      String json = EntityUtils.toString(response.getEntity(), UTF_8);
      String fetchedContentVerifiedLink = JsonBrowser.parse(json)
          .get("actions")
          .index(0)
          .get("navigateAction")
          .get("endpoint")
          .get("urlEndpoint")
          .get("url")
          .text();
      if (fetchedContentVerifiedLink != null) {
        return loadTrackInfoFromMainPage(httpInterface, fetchedContentVerifiedLink.substring(9));
      }

      log.error("Did not receive requested content verified link on track {} response: {}", videoId, json);
    }

    throw new FriendlyException("Track requires content verification.", SUSPICIOUS,
        new IllegalStateException("Expected response is not present."));
  }

  protected YoutubeTrackJsonData augmentWithPlayerScript(
      YoutubeTrackJsonData data,
      HttpInterface httpInterface,
      boolean requireFormats
  ) throws IOException {
    long now = System.currentTimeMillis();

    if (data.playerScriptUrl != null) {
      cachedPlayerScript = new CachedPlayerScript(data.playerScriptUrl, now);
      return data;
    } else if (!requireFormats) {
      return data;
    }

    CachedPlayerScript cached = cachedPlayerScript;

    if (cached != null && cached.timestamp + 600000L >= now) {
      return data.withPlayerScriptUrl(cached.playerScriptUrl);
    }

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com"))) {
      HttpClientTools.assertSuccessWithContent(response, "youtube root");

      String responseText = EntityUtils.toString(response.getEntity());
      String encodedUrl = DataFormatTools.extractBetween(responseText, "\"PLAYER_JS_URL\":\"", "\"");

      if (encodedUrl == null) {
        throw throwWithDebugInfo(log, null, "no PLAYER_JS_URL in youtube root", "html", responseText);
      }

      String fetchedPlayerScript = JsonBrowser.parse("{\"url\":\"" + encodedUrl + "\"}").get("url").text();
      cachedPlayerScript = new CachedPlayerScript(fetchedPlayerScript, now);

      return data.withPlayerScriptUrl(fetchedPlayerScript);
    }
  }

  protected static class CachedPlayerScript {
    public final String playerScriptUrl;
    public final long timestamp;

    public CachedPlayerScript(String playerScriptUrl, long timestamp) {
      this.playerScriptUrl = playerScriptUrl;
      this.timestamp = timestamp;
    }
  }
}
