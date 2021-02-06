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

  public PlaybackAudioTrackFactory() {
    this(null);
  }

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
    AudioSource source = determineSource(request);

    PlaybackAudioTrack track = new PlaybackAudioTrack(
        trackInfo,
        source.createPlayback(trackInfo),
        configuration,
        frameBufferFactory,
        source,
        executorService
    );

    if (request.getInitialPosition() > 0) {
      track.setPosition(request.getInitialPosition());
    }

    if (request.getInitialMarker() != null) {
      track.setMarker(request.getInitialMarker());
    }

    track.setUserData(request.getUserData());

    return track;
  }

  @Override
  public void close() {
    ExecutorTools.shutdownExecutor(executorService, "playback");
  }

  private AudioSource determineSource(AudioTrackRequest request) {
    AudioSource explicitSource = request.getSource();

    if (explicitSource == null) {
      if (sourceRegistry != null) {
        String sourceName = request.getTrackInfo().getSourceName();
        AudioSource source = sourceRegistry.findSource(sourceName);

        if (source == null) {
          throw new IllegalStateException("Track is not playable, its source " + sourceName + " is not registered.");
        }

        return source;
      } else {
        throw new IllegalStateException("Track is not playable, explicit source not given and no source registry.");
      }
    } else {
      return explicitSource;
    }
  }
}
