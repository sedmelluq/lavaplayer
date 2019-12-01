package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class DefaultSoundCloudHtmlDataLoader implements SoundCloudHtmlDataLoader {
  @Override
  public JsonBrowser load(HttpInterface httpInterface, String url) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
        return JsonBrowser.NULL_BROWSER;
      }

      HttpClientTools.assertSuccessWithContent(response, "video page response");

      String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      String rootData = extractJsonFromHtml(html);

      if (rootData == null) {
        throw new FriendlyException("This url does not appear to be a playable track.", SUSPICIOUS, null);
      }

      return JsonBrowser.parse(rootData);
    }
  }

  protected String extractJsonFromHtml(String html) {
    return DataFormatTools.extractBetween(html, "catch(t){}})},", ");</script>");
  }
}
