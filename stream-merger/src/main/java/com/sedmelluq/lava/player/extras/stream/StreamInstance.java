package com.sedmelluq.lava.player.extras.stream;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class StreamInstance {
  private static final Logger log = LoggerFactory.getLogger(StreamInstance.class);

  private final AudioTrack track;
  private final AudioPlayer trackPlayer;
  private final int maximumFrameCount;
  private final AudioFrame[] ringBuffer;
  private final Set<Cursor> cursors;
  private boolean initialized;
  private boolean destroyed;
  private int absoluteOffset;
  private int frameCount;

  public StreamInstance(AudioTrack track, AudioPlayer trackPlayer, int maximumFrameCount) {
    this.track = track;
    this.trackPlayer = trackPlayer;
    this.maximumFrameCount = maximumFrameCount;
    this.ringBuffer = new AudioFrame[maximumFrameCount];
    this.cursors = new HashSet<>();
  }

  public AudioTrack getTrack() {
    return track;
  }

  public synchronized Cursor createCursor(Consumer<Cursor> detachListener) {
    if (destroyed) {
      return null;
    }

    initialize();

    Cursor cursor = new Cursor(getFreshOffset(), detachListener);
    cursors.add(cursor);
    return cursor;
  }

  public synchronized void shutdown() {
    destroy();
  }

  private void destroy() {
    if (!destroyed) {
      destroyed = true;

      for (Cursor cursor : new ArrayList<>(cursors)) {
        cursor.detach();
        cursor.close();
      }

      if (initialized) {
        log.debug("Shutting down centralized stream for {}.", track.getInfo());
        trackPlayer.destroy();
      }
    }
  }

  private void initialize() {
    if (!initialized) {
      initialized = true;

      log.debug("Initializing centralized stream for {}.", track.getInfo());

      trackPlayer.addListener(new Listener());
      trackPlayer.playTrack(track);
    }
  }

  private synchronized AudioFrame getFrame(Cursor cursor) {
    if (destroyed) {
      return null;
    }

    if (cursor.absoluteOffset < 0 || cursor.absoluteOffset < absoluteOffset) {
      cursor.absoluteOffset = getFreshOffset();
    }

    if (cursor.absoluteOffset >= absoluteOffset + frameCount) {
      AudioFrame newFrame = trackPlayer.provide();

      if (newFrame == null) {
        return null;
      }

      absoluteOffset++;
      frameCount = Math.min(frameCount + 1, ringBuffer.length);

      int framePosition = getRelativeOffset(absoluteOffset + frameCount - 1);
      ringBuffer[framePosition] = newFrame;

      cursor.absoluteOffset++;
      return newFrame;
    } else {
      int framePosition = getRelativeOffset(cursor.absoluteOffset);
      cursor.absoluteOffset++;
      return ringBuffer[framePosition];
    }
  }

  private synchronized void releaseCursor(Cursor cursor) {
    if (cursors.remove(cursor) && cursors.isEmpty()) {
      destroy();
    }
  }

  private long getFreshOffset() {
    return absoluteOffset + Math.max(0, frameCount - 1);
  }

  private int getRelativeOffset(long absoluteOffset) {
    return (int) (absoluteOffset % maximumFrameCount);
  }

  public class Cursor {
    private final Consumer<Cursor> detachListener;
    private final AtomicBoolean detached = new AtomicBoolean();
    private long absoluteOffset;

    private Cursor(long absoluteOffset, Consumer<Cursor> detachListener) {
      this.detachListener = detachListener;
      this.absoluteOffset = absoluteOffset;
    }

    public AudioTrack getTrack() {
      return track;
    }

    public void close() {
      detach();
      releaseCursor(this);
    }

    public AudioFrame provide() {
      return StreamInstance.this.getFrame(this);
    }

    private void detach() {
      if (detached.compareAndSet(false, true)) {
        detachListener.accept(this);
      }
    }
  }

  private class Listener extends AudioEventAdapter {
    @Override
    public void onEvent(AudioEvent event) {
      if (event instanceof TrackEndEvent || event instanceof TrackStuckEvent) {
        destroy();
      }
    }
  }
}
