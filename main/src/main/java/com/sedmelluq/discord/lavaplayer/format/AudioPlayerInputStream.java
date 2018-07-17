package com.sedmelluq.discord.lavaplayer.format;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides an audio player as an input stream. When nothing is playing, it returns silence instead of blocking.
 */
public class AudioPlayerInputStream extends InputStream {
  private final AudioPlayer player;
  private final AudioDataFormat format;
  private final long timeout;
  private final boolean provideSilence;
  private ByteBuffer current;

  /**
   * @param format Format of the frames expected from the player
   * @param player The player to read frames from
   * @param timeout Timeout till track stuck event is sent. Each time a new frame is required from the player, it asks
   *                for a frame with the specified timeout. In case that timeout is reached, the track stuck event is
   *                sent and if providing silence is enabled, silence is provided as the next frame.
   * @param provideSilence True if the stream should return silence instead of blocking in case nothing is playing or
   *                       read times out.
   */
  public AudioPlayerInputStream(AudioDataFormat format, AudioPlayer player, long timeout, boolean provideSilence) {
    this.format = format;
    this.player = player;
    this.timeout = timeout;
    this.provideSilence = provideSilence;
  }

  @Override
  public int read() throws IOException {
    ensureAvailable();
    return current.get();
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    int currentOffset = offset;

    while (currentOffset < length) {
      ensureAvailable();

      int piece = Math.min(current.remaining(), length - currentOffset);
      current.get(buffer, currentOffset, piece);
      currentOffset += piece;
    }

    return currentOffset - offset;
  }

  @Override
  public int available() throws IOException {
    return current != null ? current.remaining() : 0;
  }

  @Override
  public void close() throws IOException {
    player.stopTrack();
  }

  /**
   * Create an instance of AudioInputStream using an AudioPlayer as a source.
   *
   * @param player Format of the frames expected from the player
   * @param format The player to read frames from
   * @param stuckTimeout Timeout till track stuck event is sent and silence is returned on reading
   * @param provideSilence Returns true if the stream should provide silence if no track is being played or when getting
   *                       track frames times out.
   * @return An audio input stream usable with JDK sound system
   */
  public static AudioInputStream createStream(AudioPlayer player, AudioDataFormat format, long stuckTimeout, boolean provideSilence) {
    AudioFormat jdkFormat = AudioDataFormatTools.toAudioFormat(format);
    return new AudioInputStream(new AudioPlayerInputStream(format, player, stuckTimeout, provideSilence), jdkFormat, -1);
  }

  private void ensureAvailable() throws IOException {
    while (available() == 0) {
      try {
        attemptRetrieveFrame();
      } catch (TimeoutException e) {
        notifyTrackStuck();
      } catch (InterruptedException e) {
        ExceptionTools.keepInterrupted(e);
        throw new InterruptedIOException();
      }

      if (available() == 0 && provideSilence) {
        addFrame(format.silenceBytes());
        break;
      }
    }
  }

  private void attemptRetrieveFrame() throws TimeoutException, InterruptedException {
    AudioFrame frame = player.provide(timeout, TimeUnit.MILLISECONDS);

    if (frame != null) {
      if (!format.equals(frame.getFormat())) {
        throw new IllegalStateException("Frame read from the player uses a different format than expected.");
      }

      addFrame(frame.getData());
    } else if (!provideSilence) {
      Thread.sleep(10);
    }
  }

  private void addFrame(byte[] data) {
    current = ByteBuffer.wrap(data);
  }

  private void notifyTrackStuck() {
    if (player instanceof TrackStateListener) {
      ((TrackStateListener) player).onTrackStuck(player.getPlayingTrack(), timeout);
    }
  }
}
