package com.sedmelluq.lavaplayer.core.source.bandcamp;

import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpConfigurable;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.playlist.BasicAudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.request.generic.GenericAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.tools.DataFormatTools;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIdentifier;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreUrl;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.FAULT;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding Bandcamp tracks based on URL.
 */
public class BandcampAudioSource implements AudioSource, HttpConfigurable {
  private static final String NAME = "bandcamp";
  private static final AudioTrackProperty sourceProperty = coreSourceName(NAME);

  private static final String TRACK_URL_REGEX = "^https?://(?:[^.]+\\.|)bandcamp\\.com/track/([a-zA-Z0-9-_]+)/?(?:\\?.*|)$";
  private static final String ALBUM_URL_REGEX = "^https?://(?:[^.]+\\.|)bandcamp\\.com/album/([a-zA-Z0-9-_]+)/?(?:\\?.*|)$";

  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);
  private static final Pattern albumUrlPattern = Pattern.compile(ALBUM_URL_REGEX);

  private final HttpInterfaceManager httpInterfaceManager;

  /**
   * Create an instance.
   */
  public BandcampAudioSource() {
    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public AudioInfoEntity loadItem(AudioInfoRequest request) {
    if (request instanceof GenericAudioInfoRequest) {
      String hint = ((GenericAudioInfoRequest) request).getHint();

      if (trackUrlPattern.matcher(hint).matches()) {
        return loadTrack(request, hint);
      } else if (albumUrlPattern.matcher(hint).matches()) {
        return loadAlbum(request, hint);
      }
    }

    return null;
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return trackInfo;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new BandcampUrlPlayback(trackInfo.getIdentifier(), this);
  }

  private AudioInfoEntity loadTrack(AudioTrackInfoTemplate template, String trackUrl) {
    return extractFromPage(trackUrl, (httpClient, text) -> {
      String bandUrl = readBandUrl(text);
      JsonBrowser trackListInfo = readTrackListInformation(text);
      String artist = trackListInfo.get("artist").safeText();

      return extractTrack(template, trackListInfo.get("trackinfo").index(0), bandUrl, artist);
    });
  }

  private AudioInfoEntity loadAlbum(AudioTrackInfoTemplate template, String albumUrl) {
    return extractFromPage(albumUrl, (httpClient, text) -> {
      String bandUrl = readBandUrl(text);
      JsonBrowser trackListInfo = readTrackListInformation(text);
      String artist = trackListInfo.get("artist").text();

      List<AudioTrackInfo> tracks = new ArrayList<>();
      for (JsonBrowser trackInfo : trackListInfo.get("trackinfo").values()) {
        tracks.add(extractTrack(template, trackInfo, bandUrl, artist));
      }

      JsonBrowser albumInfo = readAlbumInformation(text);
      return new BasicAudioPlaylist(albumInfo.get("album_title").text(), tracks, null, false);
    });
  }

  private AudioTrackInfo extractTrack(
      AudioTrackInfoTemplate template,
      JsonBrowser trackInfo,
      String bandUrl,
      String artist
  ) {
    String trackPageUrl = bandUrl + trackInfo.get("title_link").text();
    long duration = (long) (trackInfo.get("duration").as(Double.class) * 1000.0);

    return AudioTrackInfoBuilder
        .fromTemplate(template)
        .with(coreSourceName(getName()))
        .with(coreIdentifier(trackPageUrl))
        .with(coreTitle(trackInfo.get("title").text()))
        .with(coreAuthor(artist))
        .with(coreLength(duration))
        .with(coreIsStream(false))
        .with(coreUrl(trackPageUrl))
        .build();
  }

  private String readBandUrl(String text) {
    String bandUrl = DataFormatTools.extractBetween(text, "var band_url = \"", "\";");

    if (bandUrl == null) {
      throw new FriendlyException("Band information not found on the Bandcamp page.", SUSPICIOUS, null);
    }

    return bandUrl;
  }

  private JsonBrowser readAlbumInformation(String text) throws IOException {
    String albumInfoJson = DataFormatTools.extractBetween(text, "var EmbedData = ", "};");

    if (albumInfoJson == null) {
      throw new FriendlyException("Album information not found on the Bandcamp page.", SUSPICIOUS, null);
    }

    albumInfoJson = albumInfoJson.replace("\" + \"", "") + "};";
    return JsonBrowser.parse(albumInfoJson);
  }

  JsonBrowser readTrackListInformation(String text) throws IOException {
    String trackInfoJson = DataFormatTools.extractBetween(text, "var TralbumData = ", "};");

    if (trackInfoJson == null) {
      throw new FriendlyException("Track information not found on the Bandcamp page.", SUSPICIOUS, null);
    }

    trackInfoJson = trackInfoJson.replace("\" + \"", "") + "};";
    return JsonBrowser.parse(trackInfoJson + "};");
  }

  private AudioInfoEntity extractFromPage(String url, AudioItemExtractor extractor) {
    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      return extractFromPageWithInterface(httpInterface, url, extractor);
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a Bandcamp track failed.", FAULT, e);
    }
  }

  private AudioInfoEntity extractFromPageWithInterface(
      HttpInterface httpInterface,
      String url,
      AudioItemExtractor extractor
  ) throws Exception {
    String responseText;

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return AudioInfoEntity.NO_INFO;
      } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new IOException("Invalid status code for track page: " + statusCode);
      }

      responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
    }

    return extractor.extract(httpInterface, responseText);
  }

  @Override
  public void close() {
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
    AudioInfoEntity extract(HttpInterface httpInterface, String text) throws Exception;
  }
}
