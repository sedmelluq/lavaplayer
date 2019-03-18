package com.sedmelluq.discord.lavaplayer.integration;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

public class PlayerManagerTestTools {
  public static AudioTrack loadTrack(AudioPlayerManager manager, String identifier) throws Exception {
    CompletableFuture<AudioTrack> result = new CompletableFuture<>();

    manager.loadItem(identifier, new FunctionalResultHandler(
        result::complete,
        (playlist) -> result.completeExceptionally(new IllegalArgumentException()),
        () -> result.completeExceptionally(new NoSuchElementException()),
        result::completeExceptionally
    ));

    return result.get(10, TimeUnit.SECONDS);
  }

  public static long consumeTrack(AudioPlayer player) throws Exception {
    ByteBuffer buffer = ByteBuffer.allocate(960 * 2 * 2);

    MutableAudioFrame frame = new MutableAudioFrame();
    frame.setBuffer(buffer);

    CRC32 crc = new CRC32();
    int count = 0;

    while (player.getPlayingTrack() != null && player.provide(frame, 10, TimeUnit.SECONDS)) {
      buffer.flip();
      crc.update(buffer.array(), buffer.position(), buffer.remaining());
      count++;
    }

    System.out.println("Consumed " + count + " samples");

    return crc.getValue();
  }
}
