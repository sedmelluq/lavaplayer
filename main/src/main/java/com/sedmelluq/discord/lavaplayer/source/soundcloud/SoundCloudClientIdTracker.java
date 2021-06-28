package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundCloudClientIdTracker {
  private static final Logger log = LoggerFactory.getLogger(SoundCloudClientIdTracker.class);

  private static final String ID_FETCH_CONTEXT_ATTRIBUTE = "sc-raw";
  private static final long CLIENT_ID_REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(1);

  private static final String PAGE_APP_SCRIPT_REGEX = "https://[A-Za-z0-9-.]+/assets/[a-f0-9-]+\\.js";
  private static final String APP_SCRIPT_CLIENT_ID_REGEX = "[^_]client_id:\"([a-zA-Z0-9-_]+)\"";
  private static final Pattern pageAppScriptPattern = Pattern.compile(PAGE_APP_SCRIPT_REGEX);
  private static final Pattern appScriptClientIdPattern = Pattern.compile(APP_SCRIPT_CLIENT_ID_REGEX);
  private static final int EXPECTED_CLIENT_SCRIPT_INDEX = 8;

  private final Object clientIdLock = new Object();
  private final HttpInterfaceManager httpInterfaceManager;
  private String clientId;
  private long lastClientIdUpdate;
  private int lastClientScriptIndex = EXPECTED_CLIENT_SCRIPT_INDEX;

  public SoundCloudClientIdTracker(HttpInterfaceManager httpInterfaceManager) {
    this.httpInterfaceManager = httpInterfaceManager;
  }

  /**
   * Updates the clientID if more than {@link #CLIENT_ID_REFRESH_INTERVAL} time has passed since last updated.
   */
  public void updateClientId() {
    synchronized (clientIdLock) {
      long now = System.currentTimeMillis();
      if (now - lastClientIdUpdate < CLIENT_ID_REFRESH_INTERVAL) {
        log.debug("Client ID was recently updated, not updating again right away.");
        return;
      }

      lastClientIdUpdate = now;
      log.info("Updating SoundCloud client ID (current is {}).", clientId);

      try {
        clientId = findClientIdFromSite();
        log.info("Updating SoundCloud client ID succeeded, new ID is {}.", clientId);
      } catch (Exception e) {
        log.error("SoundCloud client ID update failed.", e);
      }
    }
  }

  public String getClientId() {
    synchronized (clientIdLock) {
      if (clientId == null) {
        updateClientId();
      }

      return clientId;
    }
  }

  public boolean isIdFetchContext(HttpClientContext context) {
    return context.getAttribute(ID_FETCH_CONTEXT_ATTRIBUTE) == Boolean.TRUE;
  }

  private String findClientIdFromSite() throws IOException {
    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      httpInterface.getContext().setAttribute(ID_FETCH_CONTEXT_ATTRIBUTE, true);

      List<String> scriptUrls = findScriptUrls(httpInterface);
      return findClientIdFromScripts(httpInterface, scriptUrls);
    }
  }

  private List<String> findScriptUrls(HttpInterface httpInterface) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://soundcloud.com"))) {
      HttpClientTools.assertSuccessWithContent(response, "main page response");

      String page = EntityUtils.toString(response.getEntity());
      Matcher matcher = pageAppScriptPattern.matcher(page);
      List<String> scriptUrls = new ArrayList<>();

      while (matcher.find()) {
        scriptUrls.add(matcher.group());
      }

      return scriptUrls;
    }
  }

  private String findClientIdFromScripts(HttpInterface httpInterface, List<String> scriptUrls) throws IOException {
    for (int index : getIndicesByDistance(lastClientScriptIndex, scriptUrls.size())) {
      String url = scriptUrls.get(index);
      String clientId = findClientIdFromApplicationScript(httpInterface, url);

      if (clientId != null) {
        if (index != lastClientScriptIndex) {
          log.info("Last known client script index changed to {}, should update default for efficiency.", index);
          lastClientScriptIndex = index;
        }

        return clientId;
      }
    }

    throw new IllegalStateException("Could not find client ID from " + scriptUrls.size() + " script candidates.");
  }

  private String findClientIdFromApplicationScript(HttpInterface httpInterface, String scriptUrl) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(scriptUrl))) {
      HttpClientTools.assertSuccessWithContent(response, "application script response");

      String page = EntityUtils.toString(response.getEntity());
      Matcher clientIdMatcher = appScriptClientIdPattern.matcher(page);

      if (clientIdMatcher.find()) {
        return clientIdMatcher.group(1);
      } else {
        return null;
      }
    }
  }

  // Returns range [0, size) ordered by distance from center
  private int[] getIndicesByDistance(int center, int size) {
    int maximumOffset = Math.max(size, center);

    int[] indices = new int[size];
    int indicesFilled = 0;

    for (int offset = 0; offset < maximumOffset; offset++) {
      {
        int forwardIndex = center + offset;

        if (forwardIndex >= 0 && forwardIndex < size) {
          indices[indicesFilled++] = forwardIndex;
        }
      }

      if (offset > 0) {
        int backwardsIndex = center - offset;

        if (backwardsIndex >= 0 && backwardsIndex < size) {
          indices[indicesFilled++] = backwardsIndex;
        }
      }
    }

    return indices;
  }
}
