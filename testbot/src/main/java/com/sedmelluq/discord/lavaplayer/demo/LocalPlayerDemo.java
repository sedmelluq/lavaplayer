package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;
import com.sedmelluq.lavaplayer.core.format.AudioPlayerInputStream;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequests;
import com.sedmelluq.lavaplayer.core.info.loader.SplitAudioInfoResponseHandler;
import com.sedmelluq.lavaplayer.core.manager.AudioPlayerManager;
import com.sedmelluq.lavaplayer.core.manager.DefaultAudioPlayerManager;
import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.AudioTrackRequestBuilder;
import com.sedmelluq.lavaplayer.core.source.AudioSourceManagers;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static com.sedmelluq.lavaplayer.core.format.StandardAudioDataFormats.COMMON_PCM_S16_BE;

public class LocalPlayerDemo {
  public static void main(String[] args) throws LineUnavailableException, IOException {
    AudioPlayerManager manager = DefaultAudioPlayerManager.createDefault();
    AudioSourceManagers.registerRemoteSources(manager);
    manager.getConfiguration().setOutputFormat(COMMON_PCM_S16_BE);

    AudioPlayer player = manager.createPlayer();

    manager.requestInfo(AudioInfoRequests.generic("ytsearch:epic soundtracks", new SplitAudioInfoResponseHandler(track -> {
      player.playTrack(new AudioTrackRequestBuilder(track));
    }, playlist -> {
      player.playTrack(new AudioTrackRequestBuilder(playlist.getTracks().get(0)));
    }, () -> {
      System.out.println("shiet");
    }, null)));

    AudioDataFormat format = manager.getConfiguration().getOutputFormat();
    AudioInputStream stream = AudioPlayerInputStream.createStream(player, format, 10000L, false);
    SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

    line.open(stream.getFormat());
    line.start();

    byte[] buffer = new byte[COMMON_PCM_S16_BE.maximumChunkSize()];
    int chunkSize;

    while ((chunkSize = stream.read(buffer)) >= 0) {
      line.write(buffer, 0, chunkSize);
    }
  }
}
