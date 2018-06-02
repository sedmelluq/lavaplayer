package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegFileLoader;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegTrackConsumer;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URIBuilder;

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
  private static final RequestConfig streamingRequestConfig = RequestConfig.custom().setConnectTimeout(10000).build();

  private final HttpInterface httpInterface;
  private final URI signedUrl;

  /**
   * @param trackInfo Track info
   * @param httpInterface HTTP interface to use for loading segments
   * @param signedUrl URI of the base stream with signature resolved
   */
  public YoutubeMpegStreamAudioTrack(AudioTrackInfo trackInfo, HttpInterface httpInterface, URI signedUrl) {
    super(trackInfo, null);

    this.httpInterface = httpInterface;
    this.signedUrl = signedUrl;

    // YouTube does not return a segment until it is ready, this might trigger a connect timeout otherwise.
    httpInterface.getContext().setRequestConfig(streamingRequestConfig);
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) {
    localExecutor.executeProcessingLoop(() -> {
      execute(localExecutor);
    }, null);
  }

  private void execute(LocalAudioTrackExecutor localExecutor) throws InterruptedException {
    TrackState state = new TrackState(signedUrl);

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

    try (YoutubePersistentHttpStream stream = new YoutubePersistentHttpStream(httpInterface, segmentUrl, Long.MAX_VALUE)) {
      if (stream.checkStatusCode() == HttpStatus.SC_NO_CONTENT || stream.getContentLength() == 0) {
        state.finished = true;
        return;
      }

      // If we were redirected, use that URL as a base for the next segment URL. Otherwise we will likely get redirected
      // again on every other request, which is inefficient (redirects across domains, the original URL is always
      // closing the connection, whereas the final URL is keep-alive).
      state.baseUrl = httpInterface.getFinalLocation();

      processSegmentStream(stream, localExecutor.getProcessingContext(), state);

      stream.releaseConnection();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void processSegmentStream(SeekableInputStream stream, AudioProcessingContext context, TrackState state) throws InterruptedException {
    MpegFileLoader file = new MpegFileLoader(stream);
    file.parseHeaders();

    state.absoluteSequence = extractAbsoluteSequenceFromEvent(file.getLastEventMessage());

    if (state.trackConsumer == null) {
      state.trackConsumer = loadAudioTrack(file, context);
    }

    MpegFileTrackProvider fileReader = file.loadReader(state.trackConsumer);
    if (fileReader == null) {
      throw new FriendlyException("Unknown MP4 format.", SUSPICIOUS, null);
    }

    fileReader.provideFrames();
  }

  private URI getNextSegmentUrl(TrackState state) {
    URIBuilder builder = new URIBuilder(state.baseUrl)
        .setParameter("rn", String.valueOf(state.relativeSequence))
        .setParameter("rbuf", "0");

    if (state.absoluteSequence != null) {
      builder.setParameter("sq", String.valueOf(state.absoluteSequence + 1));
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
    private URI baseUrl;

    public TrackState(URI baseUrl) {
      this.baseUrl = baseUrl;
    }
  }
}
