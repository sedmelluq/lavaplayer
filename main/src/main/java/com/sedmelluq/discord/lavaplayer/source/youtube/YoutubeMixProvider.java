package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import static com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter.PBJ_PARAMETER;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Handles loading of YouTube mixes.
 */
public class YoutubeMixProvider implements YoutubeMixLoader {
  /**
   * Loads tracks from mix in parallel into a playlist entry.
   *
   * @param mixId ID of the mix
   * @param selectedVideoId Selected track, {@link AudioPlaylist#getSelectedTrack()} will return this.
   * @return Playlist of the tracks in the mix.
   */
  public AudioPlaylist load(
      HttpInterface httpInterface,
      String mixId,
      String selectedVideoId,
      Function<AudioTrackInfo, AudioTrack> trackFactory
  ) {
    String playlistTitle = "YouTube mix";
    List<AudioTrack> tracks = new ArrayList<>();

    String mixUrl = "https://www.youtube.com/watch?v=" + selectedVideoId + "&list=" + mixId + PBJ_PARAMETER;

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(mixUrl))) {
      HttpClientTools.assertSuccessWithContent(response, "mix response");

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

      extractPlaylistTracks(playlist.get("contents"), tracks, trackFactory);
    } catch (IOException e) {
      throw new FriendlyException("Could not read mix page.", SUSPICIOUS, e);
    }

    if (tracks.isEmpty()) {
      throw new FriendlyException("Could not find tracks from mix.", SUSPICIOUS, null);
    }

    AudioTrack selectedTrack = findSelectedTrack(tracks, selectedVideoId);
    return new BasicAudioPlaylist(playlistTitle, tracks, selectedTrack, false);
  }

  private void extractPlaylistTracks(
      JsonBrowser browser,
      List<AudioTrack> tracks,
      Function<AudioTrackInfo, AudioTrack> trackFactory
  ) {
    for (JsonBrowser video : browser.values()) {
      JsonBrowser renderer = video.get("playlistPanelVideoRenderer");
      String title = renderer.get("title").get("simpleText").text();
      String author = renderer.get("longBylineText").get("runs").index(0).get("text").text();
      String durationStr = renderer.get("lengthText").get("simpleText").text();
      long duration = parseDuration(durationStr);
      String identifier = renderer.get("videoId").text();
      String uri = "https://youtube.com/watch?v=" + identifier;

      AudioTrackInfo trackInfo = new AudioTrackInfo(title, author, duration, identifier, false, uri);
      tracks.add(trackFactory.apply(trackInfo));
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

  private AudioTrack findSelectedTrack(List<AudioTrack> tracks, String selectedVideoId) {
    if (selectedVideoId != null) {
      for (AudioTrack track : tracks) {
        if (selectedVideoId.equals(track.getIdentifier())) {
          return track;
        }
      }
    }

    return null;
  }
}
