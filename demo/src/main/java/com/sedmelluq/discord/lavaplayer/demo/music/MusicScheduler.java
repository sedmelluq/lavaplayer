package com.sedmelluq.discord.lavaplayer.demo.music;

import com.sedmelluq.discord.lavaplayer.demo.MessageDispatcher;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.entities.Message;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MusicScheduler extends AudioEventAdapter implements Runnable {
  private final AudioPlayer player;
  private final MessageDispatcher messageDispatcher;
  private final ScheduledExecutorService executorService;
  private final BlockingDeque<AudioTrack> queue;
  private final AtomicReference<Message> boxMessage;

  public MusicScheduler(AudioPlayer player, MessageDispatcher messageDispatcher, ScheduledExecutorService executorService) {
    this.player = player;
    this.messageDispatcher = messageDispatcher;
    this.executorService = executorService;
    this.queue = new LinkedBlockingDeque<>();
    this.boxMessage = new AtomicReference<>();

    executorService.scheduleAtFixedRate(this, 3000L, 3000L, TimeUnit.MILLISECONDS);
  }

  public void addToQueue(AudioTrack audioTrack) {
    queue.addLast(audioTrack);
    startNextTrack(true);
  }

  public void playNow(AudioTrack audioTrack, boolean clearQueue) {
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
    AudioTrack next = queue.pollFirst();

    if (next != null) {
      if (!player.startTrack(next, noInterrupt)) {
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
  public void onTrackEnd(AudioPlayer player, AudioTrack track, boolean interrupted) {
    if (!interrupted) {
      messageDispatcher.sendMessage(String.format("Track %s finished.", track.getInfo().title));
      startNextTrack(true);
    }
  }

  @Override
  public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    messageDispatcher.sendMessage(String.format("Track %s got stuck, skipping.", track.getInfo().title));

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
        message.deleteMessage();
      }
    }

    if (track != null) {
      Message message = boxMessage.get();
      String box = TrackBoxBuilder.buildTrackBox(80, track, player.isPaused(), player.getVolume());

      if (message != null) {
        message.updateMessageAsync(box, null);
      } else {
        boxMessage.compareAndSet(null, messageDispatcher.sendMessage(box));
      }
    }
  }

  @Override
  public void run() {
    updateTrackBox(false);
  }
}
