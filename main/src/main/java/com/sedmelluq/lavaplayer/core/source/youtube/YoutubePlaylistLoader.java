package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;

public interface YoutubePlaylistLoader {
  void setPlaylistPageCount(int playlistPageCount);

  AudioPlaylist load(
      HttpInterface httpInterface,
      String playlistId,
      String selectedVideoId,
      AudioTrackInfoTemplate template
  );
}
