package com.sedmelluq.lava.player.extras.stream;

import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.REPLACED;

public class StreamAudioPlayer implements AudioPlayer {
  private static final Logger log = LoggerFactory.getLogger(StreamAudioPlayer.class);

  private final AudioPlayer fallback;
  private final StreamAudioPlayerManager manager;
  private final Object lock;
  private final List<AudioEventListener> listeners;
  private final DetachListener detachListener;
  private StreamInstance.Cursor streamCursor;

  public StreamAudioPlayer(AudioPlayer fallback, StreamAudioPlayerManager manager) {
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
  public void playTrack(AudioTrack track) {
    startTrack(track, false);
  }

  @Override
  public boolean startTrack(AudioTrack track, boolean noInterrupt) {
    if (track == null) {
      stopTrack();
    } else {
      synchronized (lock) {
        AudioTrack previousTrack = getPlayingTrack();

        if (noInterrupt && previousTrack != null) {
          return false;
        }

        if (previousTrack != null) {
          if (streamCursor == null) {
            fallback.stopTrack();
          } else {
            detachStream();
          }

          dispatchEvent(new TrackEndEvent(this, previousTrack, REPLACED));
        }

        streamCursor = manager.openTrack(track, detachListener);

        if (streamCursor == null) {
          fallback.startTrack(track, false);
        }

        dispatchEvent(new TrackStartEvent(this, track));
      }
    }

    return true;
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
  public int getVolume() {
    return fallback.getVolume();
  }

  @Override
  public void setVolume(int volume) {
    fallback.setVolume(volume);
  }

  @Override
  public void setFilterFactory(PcmFilterFactory factory) {
    fallback.setFilterFactory(factory);
  }

  @Override
  public void setFrameBufferDuration(Integer duration) {
    fallback.setFrameBufferDuration(duration);
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
  public void destroy() {
    synchronized (lock) {
      if (streamCursor != null) {
        streamCursor.close();
        streamCursor = null;
      }

      fallback.destroy();
    }
  }

  @Override
  public void addListener(AudioEventListener listener) {
    synchronized (lock) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeListener(AudioEventListener listener) {
    synchronized (lock) {
      listeners.removeIf(audioEventListener -> audioEventListener == listener);
    }
  }

  @Override
  public void checkCleanup(long threshold) {

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

  private void dispatchEvent(AudioEvent event) {
    synchronized (lock) {
      for (AudioEventListener listener : listeners) {
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

  private class StreamEventListener extends AudioEventAdapter {
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
      log.debug("Received start event from delegate player for track {}.", track.getIdentifier());
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
      dispatchEvent(new TrackStuckEvent(StreamAudioPlayer.this, track, thresholdMs, null));
    }
  }
}
