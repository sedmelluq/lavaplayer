package com.sedmelluq.discord.lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;

public class LocalPlayerDemo {
  public static void main(String[] args) throws LineUnavailableException, IOException {
    AudioPlayerManager manager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(manager);
    manager.getConfiguration().setOutputFormat(new AudioDataFormat(2, 44100, 960, AudioDataFormat.Codec.PCM_S16_BE));

    AudioPlayer player = manager.createPlayer();

    manager.loadItem("ytsearch: epic soundtracks", new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        player.playTrack(track);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack track = playlist.getSelectedTrack() != null ? playlist.getSelectedTrack() : playlist.getTracks().get(0);
        player.playTrack(track);
      }

      @Override
      public void noMatches() {

      }

      @Override
      public void loadFailed(FriendlyException exception) {

      }
    });

    AudioDataFormat format = manager.getConfiguration().getOutputFormat();
    AudioInputStream stream = AudioPlayerInputStream.createStream(player, format, 10000L, false);
    SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

    line.open(stream.getFormat());
    line.start();

    byte[] buffer = new byte[format.bufferSize(2)];

    while (true) {
      int read = stream.read(buffer);
      if (read < 0) {
        break;
      }

      line.write(buffer, 0, read);
    }
  }
}
