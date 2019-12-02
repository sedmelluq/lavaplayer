package com.sedmelluq.lavaplayer.core.player;

import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEvent;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEventListener;
import com.sedmelluq.lavaplayer.core.player.event.TrackEndEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackStartEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Triggers cleanup checks on all active audio players at a fixed interval.
 */
public class AudioPlayerLifecycleManager implements Runnable, AudioPlayerEventListener, AutoCloseable {
  private static final long CHECK_INTERVAL = 10000;
  private static final ScheduledFuture<?> DUMMY_FUTURE = createDummyFuture();

  private final ConcurrentMap<AudioPlayer, AudioPlayer> activePlayers;
  private final ScheduledExecutorService executorService;
  private final AtomicReference<ScheduledFuture<?>> scheduledTask;

  public AudioPlayerLifecycleManager() {
    this.activePlayers = new ConcurrentHashMap<>();
    this.executorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("lifecycle"));
    this.scheduledTask = new AtomicReference<>(DUMMY_FUTURE);
  }

  @Override
  public void onEvent(AudioPlayerEvent event) {
    if (event instanceof TrackStartEvent) {
      initialiseTask();
      activePlayers.put(event.player, event.player);
    } else if (event instanceof TrackEndEvent) {
      activePlayers.remove(event.player);
    }
  }

  @Override
  public void run() {
    for (AudioPlayer player : activePlayers.keySet()) {
      player.checkCleanup();
    }
  }

  @Override
  public void close() {
    ScheduledFuture<?> task = scheduledTask.getAndSet(null);

    if (task != null) {
      task.cancel(false);
    }
  }

  private void initialiseTask() {
    if (scheduledTask.get() == null) {
      ScheduledFuture<?> task = executorService.scheduleAtFixedRate(this, CHECK_INTERVAL, CHECK_INTERVAL,
          TimeUnit.MILLISECONDS);

      if (!scheduledTask.compareAndSet(DUMMY_FUTURE, task)) {
        task.cancel(false);
      }
    }
  }

  private static ScheduledFuture<Object> createDummyFuture() {
    return new ScheduledFuture<Object>() {
      @Override
      public long getDelay(TimeUnit unit) {
        return 0;
      }

      @Override
      public int compareTo(Delayed o) {
        return 0;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return false;
      }

      @Override
      public Object get() {
        return null;
      }

      @Override
      public Object get(long timeout, TimeUnit unit) {
        return null;
      }
    };
  }
}
