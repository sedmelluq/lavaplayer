package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubeUrlReader.LiveStreamHandler;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;

public class YoutubeMpegStreamUrlReader {
  private final HttpInterface httpInterface;
  private final URI signedUrl;
  private final LiveStreamHandler handler;

  public YoutubeMpegStreamUrlReader(HttpInterface httpInterface, URI signedUrl, LiveStreamHandler handler) {
    this.httpInterface = httpInterface;
    this.signedUrl = signedUrl;
    this.handler = handler;
  }

  public void read() {
    TrackState state = new TrackState(signedUrl);

    while (!state.finished) {
      processNextSegment(state);
      state.relativeSequence++;
    }
  }

  private void processNextSegment(TrackState state) {
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

      Long updatedAbsoluteSequence = handler.consumeSegmentStream(state.baseUrl, stream);

      if (updatedAbsoluteSequence != null) {
        state.absoluteSequence = updatedAbsoluteSequence;
      }

      stream.releaseConnection();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  private static class TrackState {
    private long relativeSequence;
    private Long absoluteSequence;
    private boolean finished;
    private URI baseUrl;

    public TrackState(URI baseUrl) {
      this.baseUrl = baseUrl;
    }
  }
}
