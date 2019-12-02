package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;

public interface SoundCloudPlaylistLoader {
  AudioPlaylist load(
      String identifier,
      HttpInterfaceManager httpInterfaceManager,
      AudioTrackInfoTemplate template
  );
}
