package com.sedmelluq.discord.lavaplayer.source.nico;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding NicoNico tracks based on URL.
 */
public class NicoAudioSourceManager implements AudioSourceManager, HttpConfigurable {
  private static final String TRACK_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)nicovideo\\.jp/watch/(sm[0-9]+)(?:\\?.*|)$";

  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);

  private final String email;
  private final String password;
  private final HttpInterfaceManager httpInterfaceManager;
  private final AtomicBoolean loggedIn;

  /**
   * @param email Site account email
   * @param password Site account password
   */
  public NicoAudioSourceManager(String email, String password) {
    this.email = email;
    this.password = password;
    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    loggedIn = new AtomicBoolean();
  }

  @Override
  public String getSourceName() {
    return "niconico";
  }

  @Override
  public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
    Matcher trackMatcher = trackUrlPattern.matcher(reference.identifier);

    if (trackMatcher.matches()) {
      return loadTrack(trackMatcher.group(1));
    }

    return null;
  }

  private AudioTrack loadTrack(String videoId) {
    checkLoggedIn();

    try (HttpInterface httpInterface = getHttpInterface()) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("http://ext.nicovideo.jp/api/getthumbinfo/" + videoId))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Unexpected response code from video info: " + statusCode);
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "", Parser.xmlParser());
        return extractTrackFromXml(videoId, document);
      }
    } catch (IOException e) {
      throw new FriendlyException("Error occurred when extracting video info.", SUSPICIOUS, e);
    }
  }

  private AudioTrack extractTrackFromXml(String videoId, Document document) {
    for (Element element : document.select(":root > thumb")) {
      String uploader = element.select("user_nickname").first().text();
      String title = element.select("title").first().text();
      long duration = DataFormatTools.durationTextToMillis(element.select("length").first().text());

      return new NicoAudioTrack(new AudioTrackInfo(title, uploader, duration, videoId, false, getWatchUrl(videoId)), this);
    }

    return null;
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    // No extra information to save
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    return new NicoAudioTrack(trackInfo, this);
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
  }

  /**
   * @return Get an HTTP interface for a playing track.
   */
  public HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    httpInterfaceManager.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    httpInterfaceManager.configureBuilder(configurator);
  }

  void checkLoggedIn() {
    synchronized (loggedIn) {
      if (loggedIn.get()) {
        return;
      }

      HttpPost loginRequest = new HttpPost("https://secure.nicovideo.jp/secure/login");

      loginRequest.setEntity(new UrlEncodedFormEntity(Arrays.asList(
          new BasicNameValuePair("mail", email),
          new BasicNameValuePair("password", password)
      ), StandardCharsets.UTF_8));

      try (HttpInterface httpInterface = getHttpInterface()) {
        try (CloseableHttpResponse response = httpInterface.execute(loginRequest)) {
          int statusCode = response.getStatusLine().getStatusCode();

          if (statusCode != 302) {
            throw new IOException("Unexpected response code " + statusCode);
          }

          Header location = response.getFirstHeader("Location");

          if (location == null || location.getValue().contains("message=")) {
            throw new FriendlyException("Login details for NicoNico are invalid.", COMMON, null);
          }

          loggedIn.set(true);
        }
      } catch (IOException e) {
        throw new FriendlyException("Exception when trying to log into NicoNico", SUSPICIOUS, e);
      }
    }
  }

  private static String getWatchUrl(String videoId) {
    return "http://www.nicovideo.jp/watch/" + videoId;
  }
}
