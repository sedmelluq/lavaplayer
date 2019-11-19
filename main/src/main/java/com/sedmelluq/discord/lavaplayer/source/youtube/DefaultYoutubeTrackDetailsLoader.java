package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DefaultYoutubeTrackDetailsLoader implements YoutubeTrackDetailsLoader {

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

      if (statusCode != 200) {
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
      } catch (Exception e) {
        throw new FriendlyException("Received unexpected response from YouTube.", SUSPICIOUS,
            new RuntimeException("Failed to parse: " + responseText, e));
      }
    }
  }

  private boolean checkStatusBlock(JsonBrowser statusBlock) {
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
    } else if ("UNPLAYABLE".equals(status) || "LOGIN_REQUIRED".equals(status)) {
      String unplayableReason = getUnplayableReason(statusBlock);
      throw new FriendlyException(unplayableReason, COMMON, null);
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
}
