package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.EntityUtils;

import javax.security.auth.login.LoginException;

import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.convertToMapLayout;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DefaultYoutubeTrackDetailsLoader implements YoutubeTrackDetailsLoader {

  private static final String CHARSET = "UTF-8";

  @Override
  public YoutubeTrackDetails loadDetails(HttpInterface httpInterface, String videoId) {
    try {
      return load(httpInterface, videoId);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private YoutubeTrackDetails load(HttpInterface httpInterface, String videoId) throws IOException {
    String url = "https://www.youtube.com/watch?v=" + videoId + "&pbj=1&hl=en";

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new IOException("Invalid status code for video page response: " + statusCode);
      }

      String responseText = EntityUtils.toString(response.getEntity(), UTF_8);

      try {
        JsonBrowser json = JsonBrowser.parse(responseText);
        JsonBrowser playerInfo = JsonBrowser.NULL_BROWSER;
        JsonBrowser statusBlock = JsonBrowser.NULL_BROWSER;

        for (JsonBrowser child : json.values()) {
          if (child.isMap()) {
            if (!child.get("player").isNull()) {
              playerInfo = child.get("player");
            } else if (!child.get("playerResponse").isNull()) {
              statusBlock = child.get("playerResponse").get("playabilityStatus");
            }
          }
        }

        if (!checkStatusBlock(statusBlock)) {
          return null;
        } else if (playerInfo.isNull()) {
          throw new RuntimeException("No player info block.");
        }

        return new DefaultYoutubeTrackDetails(videoId, playerInfo);
      } catch (FriendlyException e) {
        throw e;
      } catch (LoginException e) {
        return new DefaultYoutubeTrackDetails(videoId, getTrackInfoFromEmbedPage(httpInterface, videoId));
      } catch (Exception e) {
        throw new FriendlyException("Received unexpected response from YouTube.", SUSPICIOUS,
            new RuntimeException("Failed to parse: " + responseText, e));
      }
    }
  }

  private boolean checkStatusBlock(JsonBrowser statusBlock) throws LoginException {
    if (statusBlock.isNull()) {
      throw new RuntimeException("No playability status block.");
    }

    String status = statusBlock.get("status").text();

    if (status == null) {
      throw new RuntimeException("No playability status field.");
    } else if ("OK".equals(status)) {
      return true;
    } else if ("ERROR".equals(status)) {
      String reason = statusBlock.get("reason").text();

      if ("Video unavailable".equals(reason)) {
        return false;
      } else {
        throw new FriendlyException(reason, COMMON, null);
      }
    } else if ("UNPLAYABLE".equals(status)) {
      String unplayableReason = getUnplayableReason(statusBlock);
      throw new FriendlyException(unplayableReason, COMMON, null);
    } else if ("LOGIN_REQUIRED".equals(status)) {
      throw new LoginException("This video may be inappropriate for some users.");
    } else {
      throw new FriendlyException("This video cannot be viewed anonymously.", COMMON, null);
    }
  }

  private String getUnplayableReason(JsonBrowser statusBlock) {
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

  private JsonBrowser getTrackInfoFromEmbedPage(HttpInterface httpInterface, String videoId) throws IOException {
    JsonBrowser basicInfo = loadTrackBaseInfoFromEmbedPage(httpInterface, videoId);
    basicInfo.put("args", loadTrackArgsFromVideoInfoPage(httpInterface, videoId, basicInfo.get("sts").text()));
    return basicInfo;
  }

  private JsonBrowser loadTrackBaseInfoFromEmbedPage(HttpInterface httpInterface, String videoId) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com/embed/" + videoId))) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Invalid status code for embed video page response: " + statusCode);
      }

      String html = IOUtils.toString(response.getEntity().getContent(), Charset.forName(CHARSET));
      String configJson = DataFormatTools.extractBetween(html, "'PLAYER_CONFIG': ", "});writeEmbed();");

      if (configJson != null) {
        return JsonBrowser.parse(configJson);
      }
    }

    throw new FriendlyException("Track information is unavailable.", SUSPICIOUS,
            new IllegalStateException("Expected player config is not present in embed page."));
  }

  private Map<String, String> loadTrackArgsFromVideoInfoPage(HttpInterface httpInterface, String videoId, String sts) throws IOException {
    String videoApiUrl = "https://youtube.googleapis.com/v/" + videoId;
    String encodedApiUrl = URLEncoder.encode(videoApiUrl, CHARSET);
    String url = "https://www.youtube.com/get_video_info?video_id=" + videoId + "&eurl=" + encodedApiUrl +
            "hl=en_GB";

    if (sts != null) {
      url += "&sts=" + sts;
    }

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Invalid status code for video info response: " + statusCode);
      }

      return convertToMapLayout(URLEncodedUtils.parse(response.getEntity()));
    }
  }
}
