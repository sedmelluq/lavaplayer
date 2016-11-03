package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A persistent HTTP stream implementation that uses the range parameter instead of HTTP headers for specifying
 * the start position at which to start reading on a new connection.
 */
public class YoutubePersistentHttpStream extends PersistentHttpStream {

  /**
   * @param httpClient The HttpClient to use for requests
   * @param contentUrl The URL of the resource
   * @param contentLength The length of the resource in bytes
   */
  public YoutubePersistentHttpStream(CloseableHttpClient httpClient, URI contentUrl, long contentLength) {
    super(httpClient, contentUrl, contentLength);
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
  protected boolean canSeekHard() {
    return true;
  }
}
