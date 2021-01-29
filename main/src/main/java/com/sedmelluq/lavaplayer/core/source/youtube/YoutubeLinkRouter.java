package com.sedmelluq.lavaplayer.core.source.youtube;

public interface YoutubeLinkRouter {
  <T> T route(String link, Routes<T> routes);

  interface Routes<T> {
    T track(String videoId);

    T playlist(String playlistId, String selectedVideoId);

    T mix(String mixId, String selectedVideoId);

    T search(String query);

    T searchMusic(String query);

    T anonymous(String videoIds);

    T none();
  }
}
