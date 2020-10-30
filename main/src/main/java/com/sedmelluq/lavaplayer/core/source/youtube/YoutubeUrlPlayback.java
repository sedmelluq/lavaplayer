package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.container.matroska.MatroskaStreamPlayback;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegFileLoader;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegStreamPlayback;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegTrackConsumer;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegTrackConsumerFactory;
import com.sedmelluq.lavaplayer.core.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.DataFormatTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class YoutubeUrlPlayback implements AudioPlayback {
  private final YoutubeAudioSource sourceManager;
  private final AudioTrackInfo trackInfo;

  public YoutubeUrlPlayback(YoutubeAudioSource sourceManager, AudioTrackInfo trackInfo) {
    this.sourceManager = sourceManager;
    this.trackInfo = trackInfo;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    new YoutubeUrlReader(sourceManager, trackInfo, new ReadHandler(controller)).read();
  }

  private static boolean isBetterFormat(YoutubeTrackFormat format, YoutubeTrackFormat other) {
    YoutubeFormatInfo info = format.getInfo();

    if (info == null) {
      return false;
    } else if (other == null) {
      return true;
    } else if (info.ordinal() != other.getInfo().ordinal()) {
      return info.ordinal() < other.getInfo().ordinal();
    } else {
      return format.getBitrate() > other.getBitrate();
    }
  }

  private static YoutubeTrackFormat selectFormat(List<YoutubeTrackFormat> formats) {
    YoutubeTrackFormat bestFormat = null;

    for (YoutubeTrackFormat format : formats) {
      if (isBetterFormat(format, bestFormat)) {
        bestFormat = format;
      }
    }

    return bestFormat;
  }

  private static class ReadHandler implements
      YoutubeUrlReader.Handler,
      YoutubeUrlReader.ReadHandler,
      YoutubeUrlReader.LiveStreamHandler
  {
    private final AudioPlaybackController controller;
    private MpegTrackConsumer trackConsumer;

    private ReadHandler(AudioPlaybackController controller) {
      this.controller = controller;
    }

    @Override
    public void handleFormats(List<YoutubeTrackFormat> formats, YoutubeUrlReader.FormatReader reader) {
      YoutubeTrackFormat selectedFormat = selectFormat(formats);

      if (selectedFormat == null) {
        StringJoiner joiner = new StringJoiner(", ");
        formats.forEach(format -> joiner.add(format.getType().toString()));
        throw new IllegalStateException("No supported audio streams available, available types: " + joiner.toString());
      }

      reader.read(selectedFormat, this);
    }

    @Override
    public void consumeStream(YoutubeTrackFormat format, URI url, SeekableInputStream stream) {
      if (format.getType().getMimeType().endsWith("/webm")) {
        new MatroskaStreamPlayback(stream).process(controller);
      } else {
        new MpegStreamPlayback(stream).process(controller);
      }
    }

    @Override
    public void handleLiveStream(YoutubeTrackFormat format, YoutubeUrlReader.LiveStreamReader reader) {
      controller.executeProcessingLoop(() -> {
        try {
          reader.read(this);
        } finally {
          if (trackConsumer != null) {
            trackConsumer.close();
          }

          trackConsumer = null;
        }
      }, null);
    }

    @Override
    public Long consumeSegmentStream(URI url, SeekableInputStream stream) {
      MpegFileLoader file = new MpegFileLoader(stream);
      file.parseHeaders();

      Long absoluteSequence = extractAbsoluteSequenceFromEvent(file.getLastEventMessage());

      if (trackConsumer == null) {
        trackConsumer = MpegTrackConsumerFactory.create(file, controller.getContext());
      }

      MpegFileTrackProvider fileReader = file.loadReader(trackConsumer);
      if (fileReader == null) {
        throw new FriendlyException("Unknown MP4 format.", SUSPICIOUS, null);
      }

      try {
        fileReader.provideFrames();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      return absoluteSequence;
    }

    private Long extractAbsoluteSequenceFromEvent(byte[] data) {
      if (data == null) {
        return null;
      }

      String message = new String(data, StandardCharsets.UTF_8);
      String sequence = DataFormatTools.extractBetween(message, "Sequence-Number: ", "\r\n");

      return sequence != null ? Long.valueOf(sequence) : null;
    }
  }
}
