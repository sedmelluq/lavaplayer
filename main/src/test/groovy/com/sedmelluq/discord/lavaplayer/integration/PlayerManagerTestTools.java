package com.sedmelluq.discord.lavaplayer.integration;

import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequests;
import com.sedmelluq.lavaplayer.core.info.loader.SplitAudioInfoResponseHandler;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.manager.AudioPlayerManager;
import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.frame.MutableAudioFrame;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

public class PlayerManagerTestTools {
  public static AudioTrackInfo loadTrack(AudioPlayerManager manager, String identifier) throws Exception {
    CompletableFuture<AudioTrackInfo> result = new CompletableFuture<>();

    manager.requestInfo(AudioInfoRequests.generic(identifier, new SplitAudioInfoResponseHandler(
        result::complete,
        (playlist) -> result.completeExceptionally(new IllegalArgumentException()),
        () -> result.completeExceptionally(new NoSuchElementException()),
        result::completeExceptionally
    )));

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
