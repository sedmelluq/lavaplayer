package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegFileLoader;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegTrackConsumer;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * YouTube segmented MPEG stream track. The base URL always gives the latest chunk. Every chunk contains the current
 * sequence number in it, which is used to get the sequence number of the next segment. This is repeated until YouTube
 * responds to a segment request with 204.
 */
public class YoutubeMpegStreamAudioTrack extends MpegAudioTrack {
  private final CloseableHttpClient httpClient;
  private final URI signedUrl;

  /**
   * @param trackInfo Track info
   * @param httpClient HTTP client to use for loading segments
   * @param signedUrl URI of the base stream with signature resolved
   */
  public YoutubeMpegStreamAudioTrack(AudioTrackInfo trackInfo, CloseableHttpClient httpClient, URI signedUrl) {
    super(trackInfo, null);

    this.httpClient = httpClient;
    this.signedUrl = signedUrl;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) {
    localExecutor.executeProcessingLoop(() -> {
      execute(localExecutor);
    }, null);
  }

  private void execute(LocalAudioTrackExecutor localExecutor) throws InterruptedException {
    TrackState state = new TrackState();

    try {
      while (!state.finished) {
        processNextSegment(localExecutor, state);
        state.relativeSequence++;
      }
    } finally {
      if (state.trackConsumer != null) {
        state.trackConsumer.close();
      }
    }
  }

  private void processNextSegment(LocalAudioTrackExecutor localExecutor, TrackState state) throws InterruptedException {
    URI segmentUrl = getNextSegmentUrl(state);

    try (YoutubePersistentHttpStream stream = new YoutubePersistentHttpStream(httpClient, segmentUrl, Long.MAX_VALUE)) {
      if (stream.checkStatusCode() == 204) {
        state.finished = true;
        return;
      }

      MpegFileLoader file = new MpegFileLoader(stream);
      file.parseHeaders();

      state.absoluteSequence = extractAbsoluteSequenceFromEvent(file.getLastEventMessage());

      if (state.trackConsumer == null) {
        state.trackConsumer = loadAudioTrack(file, localExecutor.getProcessingContext());
      }

      MpegFileTrackProvider fileReader = file.loadReader(state.trackConsumer);
      if (fileReader == null) {
        throw new FriendlyException("Unknown MP4 format.", SUSPICIOUS, null);
      }

      fileReader.provideFrames();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private URI getNextSegmentUrl(TrackState state) {
    URIBuilder builder = new URIBuilder(signedUrl)
        .addParameter("rn", String.valueOf(state.relativeSequence))
        .addParameter("rbuf", "0");

    if (state.absoluteSequence != null) {
      builder.addParameter("sq", String.valueOf(state.absoluteSequence + 1));
    }

    try {
      return builder.build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private Long extractAbsoluteSequenceFromEvent(byte[] data) {
    if (data == null) {
      return null;
    }

    String message = new String(data, StandardCharsets.UTF_8);
    String sequence = DataFormatTools.extractBetween(message, "Sequence-Number: ", "\r\n");

    return sequence != null ? Long.valueOf(sequence) : null;
  }

  private static class TrackState {
    private long relativeSequence;
    private Long absoluteSequence;
    private MpegTrackConsumer trackConsumer;
    private boolean finished;
  }
}
