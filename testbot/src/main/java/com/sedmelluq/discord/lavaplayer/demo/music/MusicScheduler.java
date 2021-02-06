package com.sedmelluq.discord.lavaplayer.demo.music;

import com.sedmelluq.discord.lavaplayer.demo.MessageDispatcher;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.AudioTrackRequestBuilder;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEventAdapter;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrack;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.dv8tion.jda.api.entities.Message;

public class MusicScheduler extends AudioPlayerEventAdapter implements Runnable {
  private final AudioPlayer player;
  private final MessageDispatcher messageDispatcher;
  private final ScheduledExecutorService executorService;
  private final BlockingDeque<AudioTrackInfo> queue;
  private final AtomicReference<Message> boxMessage;
  private final AtomicBoolean creatingBoxMessage;

  public MusicScheduler(AudioPlayer player, MessageDispatcher messageDispatcher, ScheduledExecutorService executorService) {
    this.player = player;
    this.messageDispatcher = messageDispatcher;
    this.executorService = executorService;
    this.queue = new LinkedBlockingDeque<>();
    this.boxMessage = new AtomicReference<>();
    this.creatingBoxMessage = new AtomicBoolean();

    executorService.scheduleAtFixedRate(this, 3000L, 15000L, TimeUnit.MILLISECONDS);
  }

  public void addToQueue(AudioTrackInfo audioTrack) {
    queue.addLast(audioTrack);
    startNextTrack(true);
  }

  public List<AudioTrackInfo> drainQueue() {
    List<AudioTrackInfo> drainedQueue = new ArrayList<>();
    queue.drainTo(drainedQueue);
    return drainedQueue;
  }

  public void playNow(AudioTrackInfo audioTrack, boolean clearQueue) {
    if (clearQueue) {
      queue.clear();
    }

    queue.addFirst(audioTrack);
    startNextTrack(false);
  }

  public void skip() {
    startNextTrack(false);
  }

  private void startNextTrack(boolean noInterrupt) {
    AudioTrackInfo next = queue.pollFirst();

    if (next != null) {
      if (player.playTrack(new AudioTrackRequestBuilder(next)
          .withReplaceExisting(!noInterrupt)) == null) {
        queue.addFirst(next);
      }
    } else {
      player.stopTrack();

      messageDispatcher.sendMessage("Queue finished.");
    }
  }

  @Override
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
    updateTrackBox(true);
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    if (endReason.mayStartNext) {
      startNextTrack(true);
      messageDispatcher.sendMessage(String.format("Track %s finished.", track.getInfo().getTitle()));
    }
  }

  @Override
  public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    messageDispatcher.sendMessage(String.format("Track %s got stuck, skipping.", track.getInfo().getTitle()));

    startNextTrack(false);
  }

  @Override
  public void onPlayerResume(AudioPlayer player) {
    updateTrackBox(false);
  }

  @Override
  public void onPlayerPause(AudioPlayer player) {
    updateTrackBox(false);
  }

  private void updateTrackBox(boolean newMessage) {
    AudioTrack track = player.getPlayingTrack();

    if (track == null || newMessage) {
      Message message = boxMessage.getAndSet(null);

      if (message != null) {
        message.delete();
      }
    }

    if (track != null) {
      Message message = boxMessage.get();
      String box = TrackBoxBuilder.buildTrackBox(80, track, player.isPaused(),
          player.getConfiguration().getVolumeLevel());

      if (message != null) {
        message.editMessage(box).queue();
      } else {
        if (creatingBoxMessage.compareAndSet(false, true)) {
          messageDispatcher.sendMessage(box, created -> {
            boxMessage.set(created);
            creatingBoxMessage.set(false);
          }, error -> {
            creatingBoxMessage.set(false);
          });
        }
      }
    }
  }

  @Override
  public void run() {
    updateTrackBox(false);
  }
}
