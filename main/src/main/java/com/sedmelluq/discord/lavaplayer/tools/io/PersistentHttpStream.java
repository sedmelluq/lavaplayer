package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;

/**
 * Use an HTTP endpoint as a stream, where the connection resetting is handled gracefully by reopening the connection
 * and using a closed stream will just reopen the connection.
 */
public class PersistentHttpStream extends SeekableInputStream implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(PersistentHttpStream.class);

  private static final long MAX_SKIP_DISTANCE = 512L * 1024L;

  private final CloseableHttpClient httpClient;
  protected final URI contentUrl;
  private CloseableHttpResponse currentResponse;
  private InputStream currentContent;
  protected long position;

  /**
   * @param httpClient The HttpClient to use for requests
   * @param contentUrl The URL of the resource
   * @param contentLength The length of the resource in bytes
   */
  public PersistentHttpStream(CloseableHttpClient httpClient, URI contentUrl, long contentLength) {
    super(contentLength, MAX_SKIP_DISTANCE);

    this.httpClient = httpClient;
    this.contentUrl = contentUrl;
    this.position = 0;
  }

  protected URI getConnectUrl() {
    return contentUrl;
  }

  protected boolean useHeadersForRange() {
    return true;
  }

  private static void validateStatusCode(HttpResponse response) {
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != 200 && statusCode != 206) {
      throw new RuntimeException("Not success status code: " + statusCode);
    }
  }

  private HttpGet getConnectRequest() {
    HttpGet request = new HttpGet(getConnectUrl());

    if (useHeadersForRange()) {
      request.setHeader(HttpHeaders.RANGE, "bytes=" + position + "-");
    }

    return request;
  }

  private void connect() throws IOException {
    if (currentResponse == null) {
      currentResponse = httpClient.execute(getConnectRequest());
      validateStatusCode(currentResponse);
      currentContent = new BufferedInputStream(currentResponse.getEntity().getContent());
    }
  }

  private void handleSocketException(SocketException exception, boolean attemptReconnect) throws IOException {
    if (!attemptReconnect || !"Connection reset".equals(exception.getMessage())) {
      throw exception;
    }

    close();
  }

  private int internalRead(boolean attemptReconnect) throws IOException {
    connect();

    try {
      int result = currentContent.read();
      if (result >= 0) {
        position++;
      }
      return result;
    } catch (SocketException e) {
      handleSocketException(e, attemptReconnect);
      return internalRead(false);
    }
  }

  @Override
  public int read() throws IOException {
    return internalRead(true);
  }

  private int internalRead(byte[] b, int off, int len, boolean attemptReconnect) throws IOException {
    connect();

    try {
      int result = currentContent.read(b, off, len);
      if (result >= 0) {
        position += result;
      }
      return result;
    } catch (SocketException e) {
      handleSocketException(e, attemptReconnect);
      return internalRead(b, off, len, false);
    }
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return internalRead(b, off, len, true);
  }

  private long internalSkip(long n, boolean attemptReconnect) throws IOException {
    connect();

    try {
      long result = currentContent.skip(n);
      if (result >= 0) {
        position += result;
      }
      return result;
    } catch (SocketException e) {
      handleSocketException(e, attemptReconnect);
      return internalSkip(n, false);
    }
  }

  @Override
  public long skip(long n) throws IOException {
    return internalSkip(n, true);
  }

  private int internalAvailable(boolean attemptReconnect) throws IOException {
    connect();

    try {
      return currentContent.available();
    } catch (SocketException e) {
      handleSocketException(e, attemptReconnect);
      return internalAvailable(false);
    }
  }

  @Override
  public int available() throws IOException {
    return internalAvailable(true);
  }

  @Override
  public synchronized void reset() throws IOException {
    throw new IOException("mark/reset not supported");
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public void close() throws IOException {
    if (currentResponse != null) {
      try {
        currentResponse.close();
      } catch (IOException e) {
        log.debug("Failed to close response.", e);
      }

      currentResponse = null;
      currentContent = null;
    }
  }

  @Override
  public long getPosition() {
    return position;
  }

  @Override
  protected void seekHard(long position) throws IOException {
    close();

    this.position = position;
  }
}
