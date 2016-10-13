package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding Soundtrack tracks based on URL.
 */
public class SoundCloudAudioSourceManager implements AudioSourceManager {
  private static final String CHARSET = "UTF-8";
  private static final String CLIENT_ID = "02gUJC0hH2ct1EGOcYXQIzRFU91c72Ea";
  private static final String URL_REGEX = "^(?:http://|https://|)(?:www\\.|)soundcloud\\.com/([a-zA-Z0-9-_]*)/([a-zA-Z0-9-_]*)(?:\\?.*|)$";

  private static final Pattern urlPattern = Pattern.compile(URL_REGEX);

  private final HttpClientBuilder httpClientBuilder;

  /**
   * Create an instance.
   */
  public SoundCloudAudioSourceManager() {
    httpClientBuilder = HttpClientTools.createSharedCookiesHttpBuilder();
  }

  @Override
  public String getSourceName() {
    return "soundcloud";
  }

  @Override
  public AudioItem loadItem(AudioPlayerManager manager, String identifier) {
    if (!urlPattern.matcher(identifier).matches()) {
      return null;
    }

    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      JsonBrowser trackJson = loadTrackInfoFromUrl(httpClient, identifier);

      String trackId = trackJson.get("id").text();
      String trackUrl = "https://api.soundcloud.com/tracks/" + trackId + "/stream?client_id=" + CLIENT_ID;

      AudioTrackInfo trackInfo = new AudioTrackInfo(
          trackJson.get("title").text(),
          trackJson.get("user").get("username").text(),
          trackJson.get("full_duration").as(Integer.class),
          trackId
      );

      return new SoundCloudAudioTrack(trackInfo, this, trackUrl);
    } catch (IOException e) {
      throw new FriendlyException("Reading page from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    output.writeUTF(((SoundCloudAudioTrack) track).getTrackUrl());
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    String trackUrl = input.readUTF();

    return new SoundCloudAudioTrack(trackInfo, this, trackUrl);
  }

  /**
   * @return A new HttpClient instance. All instances returned from this method use the same cookie jar.
   */
  public CloseableHttpClient createHttpClient() {
    return httpClientBuilder.build();
  }

  private JsonBrowser loadTrackInfoFromUrl(CloseableHttpClient httpClient, String url) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == 404) {
        throw new FriendlyException("That track does not exist.", COMMON, null);
      } else if (statusCode != 200) {
        throw new IOException("Invalid status code for video page response.");
      }

      String html = IOUtils.toString(response.getEntity().getContent(), Charset.forName(CHARSET));
      String configJson = DataFormatTools.extractBetween(html, "e}var c=", ",o=Date.now()");

      if (configJson == null) {
        throw new FriendlyException("This url does not appear to be a playable track.", SUSPICIOUS, null);
      }

      return loadTrackInfoFromJson(JsonBrowser.parse(configJson));
    }
  }

  private JsonBrowser loadTrackInfoFromJson(JsonBrowser json) {
    for (JsonBrowser value : json.values()) {
      if ("64".equals(value.get("id").text())) {
        return value.get("data").index(0);
      }
    }

    throw new IllegalStateException("Could not find data 64.");
  }
}
