package com.sedmelluq.lavaplayer.core.container.mpeg;

import com.sedmelluq.lavaplayer.core.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class MpegStreamPlayback implements AudioPlayback {
  private final SeekableInputStream inputStream;

  public MpegStreamPlayback(SeekableInputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    MpegFileLoader file = new MpegFileLoader(inputStream);
    file.parseHeaders();

    MpegTrackConsumer trackConsumer = MpegTrackConsumerFactory.create(file, controller.getContext());

    try {
      MpegFileTrackProvider fileReader = file.loadReader(trackConsumer);
      if (fileReader == null) {
        throw new FriendlyException("Unknown MP4 format.", SUSPICIOUS, null);
      }

      controller.updateDuration(fileReader.getDuration());
      controller.executeProcessingLoop(fileReader::provideFrames, fileReader::seekToTimecode);
    } finally {
      trackConsumer.close();
    }
  }
}
