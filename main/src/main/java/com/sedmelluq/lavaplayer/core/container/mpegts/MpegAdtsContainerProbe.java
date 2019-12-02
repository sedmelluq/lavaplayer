package com.sedmelluq.lavaplayer.core.container.mpegts;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerHints;
import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.container.adts.AdtsStreamReader;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.tools.io.SavedHeadSeekableInputStream;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.supportedFormat;

public class MpegAdtsContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(MpegAdtsContainerProbe.class);

  @Override
  public String getName() {
    return "mpegts-adts";
  }

  @Override
  public boolean matchesHints(MediaContainerHints hints) {
    return "ts".equalsIgnoreCase(hints.fileExtension);
  }

  @Override
  public MediaContainerDetectionResult probe(AudioInfoRequest request, SeekableInputStream inputStream)
      throws IOException {

    SavedHeadSeekableInputStream head = inputStream instanceof SavedHeadSeekableInputStream ?
        (SavedHeadSeekableInputStream) inputStream : null;

    if (head != null) {
      head.setAllowDirectReads(false);
    }

    MpegTsElementaryInputStream tsStream = new MpegTsElementaryInputStream(inputStream, MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM);
    PesPacketInputStream pesStream = new PesPacketInputStream(tsStream);
    AdtsStreamReader reader = new AdtsStreamReader(pesStream);

    try {
      if (reader.findPacketHeader() != null) {
        log.debug("Track {} is an MPEG-TS stream with an ADTS track.", request.name());

        return supportedFormat(this,
            AudioTrackInfoBuilder.fromTemplate(request)
                .withProvider(inputStream)
                .withProvider(tsStream)
        );
      }
    } catch (IndexOutOfBoundsException ignored) {
      // TS stream read too far and still did not find required elementary stream - SavedHeadSeekableInputStream throws
      // this because we disabled reads past the loaded "head".
    } finally {
      if (head != null) {
        head.setAllowDirectReads(true);
      }
    }

    return null;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new MpegAdtsStreamPlayback(trackInfo.getIdentifier(), inputStream);
  }
}
