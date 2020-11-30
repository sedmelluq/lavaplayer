package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

public class YoutubeHttpContextFilter implements HttpContextFilter {
  @Override
  public void onContextOpen(HttpClientContext context) {
    CookieStore cookieStore = context.getCookieStore();

    if (cookieStore == null) {
      cookieStore = new BasicCookieStore();
      context.setCookieStore(cookieStore);
    }

    // Reset cookies for each sequence of requests.
    cookieStore.clear();
  }

  @Override
  public void onContextClose(HttpClientContext context) {
    
  }

  @Override
  public void onRequest(HttpClientContext context, HttpUriRequest request, boolean isRepetition) {
    request.setHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/76.0.3809.100 Safari/537.36");
    request.setHeader("x-youtube-client-name", "1");
    request.setHeader("x-youtube-client-version", "2.20191008.04.01");
    request.setHeader("x-youtube-page-cl", "276511266");
    request.setHeader("x-youtube-page-label", "youtube.ytfe.desktop_20191024_3_RC0");
    request.setHeader("x-youtube-utc-offset", "0");
    request.setHeader("x-youtube-variants-checksum", "7a1198276cf2b23fc8321fac72aa876b");
    request.setHeader("accept-language", "en");
  }

  @Override
  public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
    if (response.getStatusLine().getStatusCode() == 429) {
      throw new FriendlyException("This IP address has been blocked by YouTube (429).", COMMON, null);
    }

    return false;
  }

  @Override
  public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
    return false;
  }
}
