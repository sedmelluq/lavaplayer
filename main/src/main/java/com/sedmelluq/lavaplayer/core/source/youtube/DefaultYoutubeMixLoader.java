package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.playlist.BasicAudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.Units;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

/**
 * Handles loading of YouTube mixes.
 */
public class DefaultYoutubeMixLoader implements YoutubeMixLoader {
  /**
   * Loads tracks from mix in parallel into a playlist entry.
   *
   * @param mixId ID of the mix
   * @param selectedVideoId Selected track, {@link AudioPlaylist#getSelectedTrack()} will return this.
   * @return Playlist of the tracks in the mix.
   */
  @Override
  public AudioInfoEntity loadMixWithId(
      HttpInterfaceManager httpInterfaceManager,
      String mixId,
      String selectedVideoId,
      AudioTrackInfoTemplate template
  ) {
    String playlistTitle = "YouTube mix";
    List<AudioTrackInfo> tracks = new ArrayList<>();

    String mixUrl = "https://www.youtube.com/watch?v=" + selectedVideoId + "&list=" + mixId + "&pbj=1";

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(mixUrl))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!HttpClientTools.isSuccessWithContent(statusCode)) {
          throw new IOException("Invalid status code for mix response: " + statusCode);
        }

        JsonBrowser body = JsonBrowser.parse(response.getEntity().getContent());
        JsonBrowser playlist = body.index(3).get("response")
            .get("contents")
            .get("twoColumnWatchNextResults")
            .get("playlist")
            .get("playlist");

        JsonBrowser title = playlist.get("title");

        if (!title.isNull()) {
          playlistTitle = title.text();
        }

        extractPlaylistTracks(playlist.get("contents"), tracks, template);
      }
    } catch (IOException e) {
      throw new FriendlyException("Could not read mix page.", SUSPICIOUS, e);
    }

    if (tracks.isEmpty()) {
      throw new FriendlyException("Could not find tracks from mix.", SUSPICIOUS, null);
    }

    AudioTrackInfo selectedTrack = findSelectedTrack(tracks, selectedVideoId);
    return new BasicAudioPlaylist(playlistTitle, tracks, selectedTrack, false);
  }

  private void extractPlaylistTracks(
      JsonBrowser browser,
      List<AudioTrackInfo> tracks,
      AudioTrackInfoTemplate template
  ) {
    for (JsonBrowser video : browser.values()) {
      JsonBrowser renderer = video.get("playlistPanelVideoRenderer");
      String title = renderer.get("title").get("simpleText").text();
      String author = renderer.get("longBylineText").get("runs").index(0).get("text").text();
      String durationStr = renderer.get("lengthText").get("simpleText").text();
      long duration = parseDuration(durationStr);
      String identifier = renderer.get("videoId").text();

      tracks.add(YoutubeTrackInfoFactory.create(template, identifier, author, title, duration, false));
    }
  }

  private long parseDuration(String duration) {
    String[] parts = duration.split(":");

    if (parts.length == 3) { // hh::mm:ss
      int hours = Integer.parseInt(parts[0]);
      int minutes = Integer.parseInt(parts[1]);
      int seconds = Integer.parseInt(parts[2]);
      return (hours * 3600000) + (minutes * 60000) + (seconds * 1000);
    } else if (parts.length == 2) { // mm:ss
      int minutes = Integer.parseInt(parts[0]);
      int seconds = Integer.parseInt(parts[1]);
      return (minutes * 60000) + (seconds * 1000);
    } else {
      return Units.DURATION_MS_UNKNOWN;
    }
  }

  private AudioTrackInfo findSelectedTrack(List<AudioTrackInfo> tracks, String selectedVideoId) {
    if (selectedVideoId != null) {
      for (AudioTrackInfo track : tracks) {
        if (selectedVideoId.equals(track.getIdentifier())) {
          return track;
        }
      }
    }

    return null;
  }

  @Override
  public void close() {
    // Nothing to do.
  }
}
