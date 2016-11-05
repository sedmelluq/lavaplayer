package com.sedmelluq.discord.lavaplayer.source.vimeo;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
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
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which detects Vimeo tracks by URL.
 */
public class VimeoAudioSourceManager implements AudioSourceManager {
  private static final String TRACK_URL_REGEX = "^https://vimeo.com/[0-9]+(?:\\?.*|)$";
  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);

  private final HttpClientBuilder httpClientBuilder;

  /**
   * Create an instance.
   */
  public VimeoAudioSourceManager() {
    httpClientBuilder = HttpClientTools.createSharedCookiesHttpBuilder();
  }

  @Override
  public String getSourceName() {
    return "vimeo";
  }

  @Override
  public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
    if (!trackUrlPattern.matcher(reference.identifier).matches()) {
      return null;
    }

    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      return loadFromTrackPage(httpClient, reference.identifier);
    } catch (IOException e) {
      throw new FriendlyException("Loading Vimeo track information failed.", SUSPICIOUS, e);
    }
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    // Nothing special to encode
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    return new VimeoAudioTrack(trackInfo, this);
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
  }

  /**
   * @return A new HttpClient instance. All instances returned from this method use the same cookie jar.
   */
  public CloseableHttpClient createHttpClient() {
    return httpClientBuilder.build();
  }

  JsonBrowser loadConfigJsonFromPageContent(String content) throws IOException {
    String configText = DataFormatTools.extractBetween(content, "window.vimeo.clip_page_config = ", "\n");

    if (configText != null) {
      return JsonBrowser.parse(configText);
    }

    return null;
  }

  private AudioItem loadFromTrackPage(CloseableHttpClient httpClient, String trackUrl) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(trackUrl))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == 404) {
        return AudioReference.NO_TRACK;
      } else if (statusCode != 200) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code is " + statusCode));
      }

      return loadTrackFromPageContent(trackUrl, IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
    }
  }

  private AudioTrack loadTrackFromPageContent(String trackUrl, String content) throws IOException {
    JsonBrowser config = loadConfigJsonFromPageContent(content);

    if (config == null) {
      throw new FriendlyException("Track information not found on the page.", SUSPICIOUS, null);
    }

    return new VimeoAudioTrack(new AudioTrackInfo(
        config.get("clip").get("title").text(),
        config.get("owner").get("display_name").text(),
        (long) (config.get("clip").get("duration").get("raw").as(Double.class) * 1000.0),
        trackUrl,
        false
    ), this);
  }
}
