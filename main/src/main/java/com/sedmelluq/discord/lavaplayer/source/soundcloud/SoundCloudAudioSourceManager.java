package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding Soundtrack tracks based on URL.
 */
public class SoundCloudAudioSourceManager implements AudioSourceManager {
  private static final Logger log = LoggerFactory.getLogger(SoundCloudAudioSourceManager.class);

  private static final String CHARSET = "UTF-8";
  private static final String CLIENT_ID = "02gUJC0hH2ct1EGOcYXQIzRFU91c72Ea";
  private static final String TRACK_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)(?:\\?.*|)$";
  private static final String PLAYLIST_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/sets/([a-zA-Z0-9-_]+)(?:\\?.*|)$";

  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);
  private static final Pattern playlistUrlPattern = Pattern.compile(PLAYLIST_URL_REGEX);

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
    if (trackUrlPattern.matcher(identifier).matches()) {
      return loadFromTrackPage(identifier);
    } else if (playlistUrlPattern.matcher(identifier).matches()) {
      return loadFromPlaylist(identifier);
    }
    return null;
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

  private AudioTrack loadFromTrackPage(String trackWebUrl) {
    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      JsonBrowser trackInfoJson = loadTrackInfoFromJson(loadPageConfigJson(httpClient, trackWebUrl));
      return buildAudioTrack(trackInfoJson);
    } catch (IOException e) {
      throw new FriendlyException("Reading page from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private AudioTrack buildAudioTrack(JsonBrowser trackInfoJson) {
    String trackId = trackInfoJson.get("id").text();
    String trackUrl = "https://api.soundcloud.com/tracks/" + trackId + "/stream?client_id=" + CLIENT_ID;

    AudioTrackInfo trackInfo = new AudioTrackInfo(
        trackInfoJson.get("title").text(),
        trackInfoJson.get("user").get("username").text(),
        trackInfoJson.get("duration").as(Integer.class),
        trackId
    );

    return new SoundCloudAudioTrack(trackInfo, this, trackUrl);
  }

  private JsonBrowser loadPageConfigJson(CloseableHttpClient httpClient, String url) throws IOException {
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

      return JsonBrowser.parse(configJson);
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

  private AudioPlaylist loadFromPlaylist(String playlistWebUrl) {
    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      JsonBrowser playlistInfo = loadPlaylistInfoFromJson(loadPageConfigJson(httpClient, playlistWebUrl));

      return new BasicAudioPlaylist(
          playlistInfo.get("title").text(),
          loadTracksFromPlaylist(httpClient, playlistInfo, playlistWebUrl),
          null
      );
    } catch (IOException e) {
      throw new FriendlyException("Reading page from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private JsonBrowser loadPlaylistInfoFromJson(JsonBrowser json) {
    for (JsonBrowser value : json.values()) {
      if ("84".equals(value.get("id").text())) {
        return value.get("data").index(0);
      }
    }

    throw new IllegalStateException("Could not find data 84.");
  }

  private List<AudioTrack> loadTracksFromPlaylist(CloseableHttpClient httpClient, JsonBrowser playlistInfo, String playlistWebUrl) throws IOException {
    List<AudioTrack> tracks = new ArrayList<>();
    URI trackListUrl = buildTrackListUrl(playlistInfo);

    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(trackListUrl))) {
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Invalid status code for track list response.");
      }

      JsonBrowser trackList = JsonBrowser.parse(response.getEntity().getContent());
      int blockedCount = 0;

      for (JsonBrowser trackInfoJson : trackList.values()) {
        if ("BLOCK".equals(trackInfoJson.get("policy").text())) {
          blockedCount++;
        } else {
          tracks.add(buildAudioTrack(trackInfoJson));
        }
      }

      if (blockedCount > 0) {
        log.debug("In soundcloud playlist {}, {} tracks were omitted because they are blocked.", playlistWebUrl, blockedCount);
      }
    }

    return tracks;
  }

  private URI buildTrackListUrl(JsonBrowser playlistInfo) {
    try {
      return new URIBuilder("https://api-v2.soundcloud.com/tracks")
          .addParameter("ids", buildTrackIdList(playlistInfo))
          .addParameter("client_id", CLIENT_ID)
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private String buildTrackIdList(JsonBrowser playlistInfo) {
    StringJoiner joiner = new StringJoiner(",");
    for (JsonBrowser trackInfo : playlistInfo.get("tracks").values()) {
      joiner.add(trackInfo.get("id").text());
    }
    return joiner.toString();
  }
}
