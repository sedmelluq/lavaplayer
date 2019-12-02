package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.playlist.BasicAudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class DefaultSoundCloudPlaylistLoader implements SoundCloudPlaylistLoader {
  private static final Logger log = LoggerFactory.getLogger(DefaultSoundCloudPlaylistLoader.class);

  protected static final String PLAYLIST_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)(?:m\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/sets/([a-zA-Z0-9-_]+)(?:\\?.*|)$";
  protected static final Pattern playlistUrlPattern = Pattern.compile(PLAYLIST_URL_REGEX);

  protected final SoundCloudHtmlDataLoader htmlDataLoader;
  protected final SoundCloudDataReader dataReader;
  protected final SoundCloudFormatHandler formatHandler;

  public DefaultSoundCloudPlaylistLoader(
      SoundCloudHtmlDataLoader htmlDataLoader,
      SoundCloudDataReader dataReader,
      SoundCloudFormatHandler formatHandler
  ) {
    this.htmlDataLoader = htmlDataLoader;
    this.dataReader = dataReader;
    this.formatHandler = formatHandler;
  }

  @Override
  public AudioPlaylist load(
      String identifier,
      HttpInterfaceManager httpInterfaceManager,
      AudioTrackInfoTemplate template
  ) {
    String url = SoundCloudHelper.nonMobileUrl(identifier);

    if (playlistUrlPattern.matcher(url).matches()) {
      return loadFromSet(httpInterfaceManager, url, template);
    } else {
      return null;
    }
  }

  protected AudioPlaylist loadFromSet(
      HttpInterfaceManager httpInterfaceManager,
      String playlistWebUrl,
      AudioTrackInfoTemplate template
  ) {
    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      JsonBrowser rootData = htmlDataLoader.load(httpInterface, playlistWebUrl);
      JsonBrowser playlistData = dataReader.findPlaylistData(rootData);

      return new BasicAudioPlaylist(
          dataReader.readPlaylistName(playlistData),
          loadPlaylistTracks(httpInterface, playlistData, template),
          null,
          false
      );
    } catch (IOException e) {
      throw new FriendlyException("Loading playlist from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  protected List<AudioTrackInfo> loadPlaylistTracks(
      HttpInterface httpInterface,
      JsonBrowser playlistData,
      AudioTrackInfoTemplate template
  ) throws IOException {
    String playlistId = dataReader.readPlaylistIdentifier(playlistData);

    List<String> trackIds = dataReader.readPlaylistTracks(playlistData).stream()
        .map(dataReader::readTrackId)
        .collect(Collectors.toList());

    List<JsonBrowser> trackDataList;

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(buildTrackListUrl(trackIds)))) {
      HttpClientTools.assertSuccessWithContent(response, "track list response");

      JsonBrowser trackList = JsonBrowser.parse(response.getEntity().getContent());
      trackDataList = trackList.values();
    }

    sortPlaylistTracks(trackDataList, trackIds);

    int blockedCount = 0;
    List<AudioTrackInfo> tracks = new ArrayList<>();

    for (JsonBrowser trackData : trackDataList) {
      if (dataReader.isTrackBlocked(trackData)) {
        blockedCount++;
      } else {
        try {
          AudioTrackInfoBuilder builder = AudioTrackInfoBuilder.fromTemplate(template)
              .with(SoundCloudHelper.sourceProperty);

          dataReader.readTrackInfo(trackData, formatHandler.buildFormatIdentifier(
              formatHandler.chooseBestFormat(dataReader.readTrackFormats(trackData))
          ), builder);

          tracks.add(builder.build());
        } catch (Exception e) {
          log.error("In soundcloud playlist {}, failed to load track", playlistId, e);
        }
      }
    }

    if (blockedCount > 0) {
      log.debug("In soundcloud playlist {}, {} tracks were omitted because they are blocked.",
          playlistId, blockedCount);
    }

    return tracks;
  }

  protected URI buildTrackListUrl(List<String> trackIds) {
    try {
      StringJoiner joiner = new StringJoiner(",");
      for (String trackId : trackIds) {
        joiner.add(trackId);
      }

      return new URIBuilder("https://api-v2.soundcloud.com/tracks")
          .addParameter("ids", joiner.toString())
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  protected void sortPlaylistTracks(List<JsonBrowser> trackDataList, List<String> trackIds) {
    Map<String, Integer> positions = new HashMap<>();

    for (int i = 0; i < trackIds.size(); i++) {
      positions.put(trackIds.get(i), i);
    }

    trackDataList.sort(Comparator.comparingInt(trackData ->
        positions.getOrDefault(dataReader.readTrackId(trackData), Integer.MAX_VALUE)
    ));
  }
}
