package com.sedmelluq.lavaplayer.core.player.playback;

import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.common.tools.ExecutorTools;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.source.AudioSourceRegistry;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackFactory;
import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.frame.AllocatingAudioFrameBuffer;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameBufferFactory;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackRequest;
import com.sedmelluq.lavaplayer.core.player.track.ExecutableAudioTrack;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PlaybackAudioTrackFactory implements AudioTrackFactory {
  private final AudioSourceRegistry sourceRegistry;
  private final AudioFrameBufferFactory frameBufferFactory;
  private final ExecutorService executorService;

  public PlaybackAudioTrackFactory(AudioSourceRegistry sourceRegistry) {
    this(
        sourceRegistry,
        AllocatingAudioFrameBuffer::new,
        createDefaultExecutor()
    );
  }

  public PlaybackAudioTrackFactory(
      AudioSourceRegistry sourceRegistry,
      AudioFrameBufferFactory frameBufferFactory,
      ExecutorService executorService
  ) {
    this.sourceRegistry = sourceRegistry;
    this.frameBufferFactory = frameBufferFactory;
    this.executorService = executorService;
  }

  public static ExecutorService createDefaultExecutor() {
    return new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10, TimeUnit.SECONDS,
        new SynchronousQueue<>(), new DaemonThreadFactory("playback"));
  }

  @Override
  public ExecutableAudioTrack create(AudioTrackRequest request, AudioConfiguration configuration) {
    AudioTrackInfo trackInfo = request.getTrackInfo();
    AudioSource sourceManager = request.getSource();

    if (sourceManager == null) {
      sourceManager = sourceRegistry.findSource(trackInfo.getSourceName());
    }

    if (sourceManager != null) {
      return new PlaybackAudioTrack(
          trackInfo,
          sourceManager.createPlayback(trackInfo),
          configuration,
          frameBufferFactory,
          sourceManager,
          executorService);
    } else {
      throw new IllegalStateException("Track is not playable, its source is not registered.");
    }
  }

  @Override
  public void close() {
    ExecutorTools.shutdownExecutor(executorService, "playback");
  }
}
