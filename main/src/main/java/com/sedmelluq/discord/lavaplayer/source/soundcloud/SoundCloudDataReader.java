package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.util.List;

public interface SoundCloudDataReader {
  JsonBrowser findTrackData(JsonBrowser rootData);

  String readTrackId(JsonBrowser trackData);

  boolean isTrackBlocked(JsonBrowser trackData);

  AudioTrackInfo readTrackInfo(JsonBrowser trackData, String identifier);

  List<SoundCloudTrackFormat> readTrackFormats(JsonBrowser trackData);

  JsonBrowser findPlaylistData(JsonBrowser rootData);

  String readPlaylistName(JsonBrowser playlistData);

  String readPlaylistIdentifier(JsonBrowser playlistData);

  List<JsonBrowser> readPlaylistTracks(JsonBrowser playlistData);
}
