package com.sedmelluq.discord.lavaplayer.source.bandcamp;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding Bandcamp tracks based on URL.
 */
public class BandcampAudioSourceManager implements AudioSourceManager, HttpConfigurable {
  private static final String URL_REGEX = "^(https?://(?:[^.]+\\.|)bandcamp\\.com)/(track|album)/([a-zA-Z0-9-_]+)/?(?:\\?.*|)$";
  private static final Pattern urlRegex = Pattern.compile(URL_REGEX);

  private final HttpInterfaceManager httpInterfaceManager;

  /**
   * Create an instance.
   */
  public BandcampAudioSourceManager() {
    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
  }

  @Override
  public String getSourceName() {
    return "bandcamp";
  }

  @Override
  public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
    UrlInfo urlInfo = parseUrl(reference.identifier);

    if (urlInfo != null) {
      if (urlInfo.isAlbum) {
        return loadAlbum(urlInfo);
      } else {
        return loadTrack(urlInfo);
      }
    }

    return null;
  }

  private UrlInfo parseUrl(String url) {
    Matcher matcher = urlRegex.matcher(url);

    if (matcher.matches()) {
      return new UrlInfo(url, matcher.group(1), "album".equals(matcher.group(2)));
    } else {
      return null;
    }
  }

  private AudioItem loadTrack(UrlInfo urlInfo) {
    return extractFromPage(urlInfo.fullUrl, (httpClient, text) -> {
      JsonBrowser trackListInfo = readTrackListInformation(text);
      String artist = trackListInfo.get("artist").safeText();

      return extractTrack(trackListInfo.get("trackinfo").index(0), urlInfo.baseUrl, artist);
    });
  }

  private AudioItem loadAlbum(UrlInfo urlInfo) {
    return extractFromPage(urlInfo.fullUrl, (httpClient, text) -> {
      JsonBrowser trackListInfo = readTrackListInformation(text);
      String artist = trackListInfo.get("artist").text();

      List<AudioTrack> tracks = new ArrayList<>();
      for (JsonBrowser trackInfo : trackListInfo.get("trackinfo").values()) {
        tracks.add(extractTrack(trackInfo, urlInfo.baseUrl, artist));
      }

      JsonBrowser albumInfo = readAlbumInformation(text);
      return new BasicAudioPlaylist(albumInfo.get("current").get("title").text(), tracks, null, false);
    });
  }

  private AudioTrack extractTrack(JsonBrowser trackInfo, String bandUrl, String artist) {
    String trackPageUrl = bandUrl + trackInfo.get("title_link").text();

    return new BandcampAudioTrack(new AudioTrackInfo(
        trackInfo.get("title").text(),
        artist,
        (long) (trackInfo.get("duration").as(Double.class) * 1000.0),
        bandUrl + trackInfo.get("title_link").text(),
        false,
        trackPageUrl
    ), this);
  }

  private JsonBrowser readAlbumInformation(String text) throws IOException {
    String albumInfoJson = DataFormatTools.extractBetween(text, "data-tralbum=\"", "\"");

    if (albumInfoJson == null) {
      throw new FriendlyException("Album information not found on the Bandcamp page.", SUSPICIOUS, null);
    }

    albumInfoJson = albumInfoJson.replace("&quot;", "\"");
    return JsonBrowser.parse(albumInfoJson);
  }

  JsonBrowser readTrackListInformation(String text) throws IOException {
    String trackInfoJson = DataFormatTools.extractBetween(text, "data-tralbum=\"", "\"");

    if (trackInfoJson == null) {
      throw new FriendlyException("Track information not found on the Bandcamp page.", SUSPICIOUS, null);
    }

    trackInfoJson = trackInfoJson.replace("&quot;", "\"");
    return JsonBrowser.parse(trackInfoJson);
  }

  private AudioItem extractFromPage(String url, AudioItemExtractor extractor) {
    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      return extractFromPageWithInterface(httpInterface, url, extractor);
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a Bandcamp track failed.", FAULT, e);
    }
  }

  private AudioItem extractFromPageWithInterface(HttpInterface httpInterface, String url, AudioItemExtractor extractor) throws Exception {
    String responseText;

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return new AudioReference(null, null);
      } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new IOException("Invalid status code for track page: " + statusCode);
      }

      responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
    }

    return extractor.extract(httpInterface, responseText);
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    // No special values to encode
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    return new BandcampAudioTrack(trackInfo, this);
  }

  @Override
  public void shutdown() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);
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

  private interface AudioItemExtractor {
    AudioItem extract(HttpInterface httpInterface, String text) throws Exception;
  }

  private static class UrlInfo {
    public final String fullUrl;
    public final String baseUrl;
    public final boolean isAlbum;

    private UrlInfo(String fullUrl, String baseUrl, boolean isAlbum) {
      this.fullUrl = fullUrl;
      this.baseUrl = baseUrl;
      this.isAlbum = isAlbum;
    }
  }
}
