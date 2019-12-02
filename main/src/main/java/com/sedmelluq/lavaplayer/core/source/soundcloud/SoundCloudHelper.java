package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.PersistentHttpStream;
import java.io.IOException;
import java.net.URI;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;

public class SoundCloudHelper {
  public static final String NAME = "soundcloud";
  public static final AudioTrackProperty sourceProperty = coreSourceName(NAME);

  public static String nonMobileUrl(String url) {
    if (url.startsWith("https://m.")) {
      return "https://" + url.substring("https://m.".length());
    } else {
      return url;
    }
  }

  public static String loadPlaybackUrl(HttpInterface httpInterface, String jsonUrl) throws IOException {
    try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, URI.create(jsonUrl), null)) {
      if (!HttpClientTools.isSuccessWithContent(stream.checkStatusCode())) {
        throw new IOException("Invalid status code for soundcloud stream: " + stream.checkStatusCode());
      }

      JsonBrowser json = JsonBrowser.parse(stream);
      return json.get("url").text();
    }
  }
}
