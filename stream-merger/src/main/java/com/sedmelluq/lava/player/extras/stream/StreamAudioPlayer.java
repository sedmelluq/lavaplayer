package com.sedmelluq.lava.player.extras.stream;

import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.AudioTrackRequestBuilder;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEvent;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEventAdapter;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEventListener;
import com.sedmelluq.lavaplayer.core.player.event.PlayerPauseEvent;
import com.sedmelluq.lavaplayer.core.player.event.PlayerResumeEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackEndEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackExceptionEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackStartEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackStuckEvent;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrame;
import com.sedmelluq.lavaplayer.core.player.frame.MutableAudioFrame;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrack;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackFactory;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackRequest;
import com.sedmelluq.lavaplayer.core.player.track.ExecutableAudioTrack;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason.REPLACED;

public class StreamAudioPlayer implements AudioPlayer {
  private static final Logger log = LoggerFactory.getLogger(StreamAudioPlayer.class);

  private final AudioTrackFactory trackFactory;
  private final AudioPlayer fallback;
  private final StreamAudioPlayerManager manager;
  private final Object lock;
  private final List<AudioPlayerEventListener> listeners;
  private final DetachListener detachListener;
  private StreamInstance.Cursor streamCursor;

  public StreamAudioPlayer(AudioTrackFactory trackFactory, AudioPlayer fallback, StreamAudioPlayerManager manager) {
    this.trackFactory = trackFactory;
    this.fallback = fallback;
    this.manager = manager;
    this.lock = new Object();
    this.listeners = new ArrayList<>();
    this.detachListener = new DetachListener();

    fallback.addListener(new StreamEventListener());
  }

  @Override
  public AudioTrack getPlayingTrack() {
    synchronized (lock) {
      if (streamCursor != null) {
        return streamCursor.getTrack();
      } else {
        return fallback.getPlayingTrack();
      }
    }
  }

  @Override
  public AudioTrack playTrack(AudioTrackRequest request) {
    ExecutableAudioTrack newTrack;
    ExecutableAudioTrack previousTrack;

    if (request == null || request.getTrackInfo() == null) {
      stopTrack();
      return null;
    } else {
      synchronized (lock) {
        previousTrack = (ExecutableAudioTrack) getPlayingTrack();

        if (!request.getReplaceExisting() && previousTrack != null) {
          return null;
        }

        newTrack = trackFactory.create(request, fallback.getConfiguration());

        if (previousTrack != null) {
          if (streamCursor == null) {
            fallback.stopTrack();
          } else {
            detachStream();
          }

          dispatchEvent(new TrackEndEvent(this, previousTrack, REPLACED));
        }

        streamCursor = manager.openTrack(newTrack, detachListener);

        if (streamCursor == null) {
          newTrack = (ExecutableAudioTrack) fallback.playTrack(new AudioTrackRequestBuilder(newTrack.getInfo())
              .withReplaceExisting(false)
              .build());
        }

        dispatchEvent(new TrackStartEvent(this, newTrack));
      }
    }

    return newTrack;
  }

  @Override
  public void stopTrack() {
    synchronized (lock) {
      if (streamCursor != null) {
        streamCursor.close();
        streamCursor = null;
      }

      fallback.stopTrack();
    }
  }

  @Override
  public boolean isPaused() {
    return fallback.isPaused();
  }

  @Override
  public void setPaused(boolean value) {
    fallback.setPaused(value);
  }

  @Override
  public void close() throws Exception {
    synchronized (lock) {
      if (streamCursor != null) {
        streamCursor.close();
        streamCursor = null;
      }

      fallback.close();
    }
  }

  @Override
  public void addListener(AudioPlayerEventListener listener) {
    synchronized (lock) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeListener(AudioPlayerEventListener listener) {
    synchronized (lock) {
      listeners.removeIf(audioEventListener -> audioEventListener == listener);
    }
  }

  @Override
  public AudioFrame provide() {
    synchronized (lock) {
      if (streamCursor != null) {
        AudioFrame frame = streamCursor.provide();

        if (frame == null) {
          if (streamCursor.getTrack() == null) {
            detachStream();
          } else {
            return null;
          }
        } else {
          return frame;
        }
      }

      return fallback.provide();
    }
  }

  @Override
  public AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
    synchronized (lock) {
      if (streamCursor != null) {
        throw new UnsupportedOperationException();
      }

      return fallback.provide(timeout, unit);
    }
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame) {
    synchronized (lock) {
      if (streamCursor != null) {
        throw new UnsupportedOperationException();
      }

      return fallback.provide(targetFrame);
    }
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame, long timeout, TimeUnit unit) throws TimeoutException,
      InterruptedException {

    synchronized (lock) {
      if (streamCursor != null) {
        throw new UnsupportedOperationException();
      }

      return fallback.provide(targetFrame, timeout, unit);
    }
  }

  private void detachStream() {
    if (streamCursor != null) {
      streamCursor.close();
      streamCursor = null;
    }
  }

  private void dispatchEvent(AudioPlayerEvent event) {
    synchronized (lock) {
      for (AudioPlayerEventListener listener : listeners) {
        try {
          listener.onEvent(event);
        } catch (Exception e) {
          log.error("Handler of event {} threw an exception.", event, e);
        }
      }
    }
  }

  private class DetachListener implements Consumer<StreamInstance.Cursor> {

    @Override
    public void accept(StreamInstance.Cursor cursor) {
      synchronized (lock) {
        if (streamCursor == cursor) {
          detachStream();
        }
      }
    }
  }

  private class StreamEventListener extends AudioPlayerEventAdapter {
    @Override
    public void onPlayerPause(AudioPlayer player) {
      dispatchEvent(new PlayerPauseEvent(StreamAudioPlayer.this));
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
      dispatchEvent(new PlayerResumeEvent(StreamAudioPlayer.this));
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
      log.debug("Received start event from delegate player for track {}.", track.getInfo().getIdentifier());
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
      if (endReason.mayStartNext || endReason == AudioTrackEndReason.CLEANUP) {
        dispatchEvent(new TrackEndEvent(StreamAudioPlayer.this, track, endReason));
      }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
      dispatchEvent(new TrackExceptionEvent(StreamAudioPlayer.this, track, exception));
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
      dispatchEvent(new TrackStuckEvent(StreamAudioPlayer.this, track, thresholdMs));
    }
  }
}
