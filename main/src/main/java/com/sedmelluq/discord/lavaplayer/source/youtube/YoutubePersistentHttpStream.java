package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A persistent HTTP stream implementation that uses the range parameter instead of HTTP headers for specifying
 * the start position at which to start reading on a new connection.
 */
public class YoutubePersistentHttpStream extends PersistentHttpStream {

  /**
   * @param httpInterface The HTTP interface to use for requests
   * @param contentUrl The URL of the resource
   * @param contentLength The length of the resource in bytes
   */
  public YoutubePersistentHttpStream(HttpInterface httpInterface, URI contentUrl, long contentLength) {
    super(httpInterface, contentUrl, contentLength);
  }

  @Override
  protected URI getConnectUrl() {
    if (position > 0) {
      try {
        return new URIBuilder(contentUrl).addParameter("range", position + "-" + contentLength).build();
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    } else {
      return contentUrl;
    }
  }

  @Override
  protected boolean useHeadersForRange() {
    return false;
  }

  @Override
  public boolean canSeekHard() {
    return true;
  }
}
