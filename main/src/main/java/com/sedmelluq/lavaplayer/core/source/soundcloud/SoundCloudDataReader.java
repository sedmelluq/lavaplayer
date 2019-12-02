package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import java.util.List;

public interface SoundCloudDataReader {
  JsonBrowser findTrackData(JsonBrowser rootData);

  String readTrackId(JsonBrowser trackData);

  boolean isTrackBlocked(JsonBrowser trackData);

  void readTrackInfo(JsonBrowser trackData, String identifier, AudioTrackInfoBuilder builder);

  List<SoundCloudTrackFormat> readTrackFormats(JsonBrowser trackData);

  JsonBrowser findPlaylistData(JsonBrowser rootData);

  String readPlaylistName(JsonBrowser playlistData);

  String readPlaylistIdentifier(JsonBrowser playlistData);

  List<JsonBrowser> readPlaylistTracks(JsonBrowser playlistData);
}
