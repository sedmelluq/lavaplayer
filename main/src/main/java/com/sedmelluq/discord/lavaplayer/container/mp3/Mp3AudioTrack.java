package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Audio track that handles an MP3 stream
 */
public class Mp3AudioTrack extends BaseAudioTrack {
  private final SeekableInputStream inputStream;

  /**
   * @param executor Track executor
   * @param trackInfo Track info
   * @param inputStream Input stream for the MP3 file
   */
  public Mp3AudioTrack(AudioTrackExecutor executor, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    super(executor, trackInfo);

    this.inputStream = inputStream;
  }

  @Override
  public void process(AtomicInteger volumeLevel) throws Exception {
    Mp3StreamingFile file = new Mp3StreamingFile(inputStream, executor.getFrameConsumer(), volumeLevel);
    file.parseHeaders();

    try {
      executor.executeProcessingLoop(file::provideFrames, file::seekToTimecode);
    } finally {
      file.close();
    }
  }
}
