package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import java.io.Closeable;

public interface YoutubeMixLoader extends Closeable {
  AudioInfoEntity loadMixWithId(
      HttpInterfaceManager httpInterfaceManager,
      String mixId,
      String selectedVideoId,
      AudioTrackInfoTemplate template
  );
}
