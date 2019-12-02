package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubePersistentHttpStream;
import com.sedmelluq.lavaplayer.core.tools.DataFormatTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegFileLoader;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegStreamPlayback;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegTrackConsumer;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegTrackConsumerFactory;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class YoutubeMpegStreamUrlPlayback extends MpegStreamPlayback {
  private final HttpInterface httpInterface;
  private final URI signedUrl;

  public YoutubeMpegStreamUrlPlayback(HttpInterface httpInterface, URI signedUrl) {
    super(null);

    this.httpInterface = httpInterface;
    this.signedUrl = signedUrl;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    controller.executeProcessingLoop(() -> {
      execute(controller);
    }, null);
  }

  private void execute(AudioPlaybackController controller) throws InterruptedException {
    TrackState state = new TrackState(signedUrl);

    try {
      while (!state.finished) {
        processNextSegment(controller.getContext(), state);
        state.relativeSequence++;
      }
    } finally {
      if (state.trackConsumer != null) {
        state.trackConsumer.close();
      }
    }
  }

  private void processNextSegment(AudioPlaybackContext context, TrackState state) throws InterruptedException {
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

      processSegmentStream(stream, context, state);

      stream.releaseConnection();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void processSegmentStream(SeekableInputStream stream, AudioPlaybackContext context, TrackState state) throws InterruptedException {
    MpegFileLoader file = new MpegFileLoader(stream);
    file.parseHeaders();

    state.absoluteSequence = extractAbsoluteSequenceFromEvent(file.getLastEventMessage());

    if (state.trackConsumer == null) {
      state.trackConsumer = MpegTrackConsumerFactory.create(file, context);
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
