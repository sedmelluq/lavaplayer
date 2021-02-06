package com.sedmelluq.lavaplayer.core.source.twitch;

import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.tools.DataFormatTools;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

public class TwitchVideoSessionManager {
  private static final long TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);

  private final HttpInterfaceManager httpInterfaceManager;
  private final AtomicReference<SessionResponse> cachedResponse = new AtomicReference<>();

  public TwitchVideoSessionManager(HttpInterfaceManager httpInterfaceManager) {
    this.httpInterfaceManager = httpInterfaceManager;
  }

  public TwitchVideoSession getSession() {
    TwitchVideoSession session = getCachedSession();

    if (session == null) {
      session = getFreshSession();
      cachedResponse.set(new SessionResponse(session, System.currentTimeMillis()));
    }

    return session;
  }

  private TwitchVideoSession getCachedSession() {
    SessionResponse response = cachedResponse.get();

    if (response != null) {
      if (response.responseTime + TIMEOUT_MS >= System.currentTimeMillis()) {
        return response.session;
      }

      cachedResponse.compareAndSet(response, null);
    }

    return null;
  }


  private TwitchVideoSession getFreshSession() {
    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      HttpGet request = new HttpGet("https://www.twitch.tv");
      request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

      try (CloseableHttpResponse response = httpInterface.execute(request)) {
        HttpClientTools.assertSuccessWithContent(response, "Twitch front page");

        String content = EntityUtils.toString(response.getEntity());
        String deviceId = extractDeviceId(response);
        String clientId = extractClientId(content);

        return new TwitchVideoSession(clientId, deviceId);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to fetch device and client id", e);
    }
  }

  private String extractDeviceId(CloseableHttpResponse response) throws IOException {
    for (Header header : response.getHeaders("Set-Cookie")) {
      String uniqueId = DataFormatTools.extractBetween(header.getValue(), "unique_id=", ";");

      if (uniqueId != null) {
        return uniqueId;
      }
    }

    throw new IOException("Could not find unique_id cookie from a Set-Cookie header.");
  }

  private String extractClientId(String responseContent) throws IOException {
    String clientId = DataFormatTools.extractBetween(responseContent, "\"Client-ID\":\"", "\"");

    if (clientId == null) {
      throw new IOException("Could not find Client-Id from response content.");
    }

    return clientId;
  }

  private static class SessionResponse {
    private final TwitchVideoSession session;
    private final long responseTime;

    private SessionResponse(TwitchVideoSession session, long responseTime) {
      this.session = session;
      this.responseTime = responseTime;
    }
  }
}
