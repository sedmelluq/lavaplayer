package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

/**
 * Base class for an HTTP interface manager with lazily initialized http client instance.
 */
public abstract class AbstractHttpInterfaceManager implements HttpInterfaceManager {
  private final HttpClientBuilder clientBuilder;
  private final Object lock;
  private boolean closed;
  private CloseableHttpClient sharedClient;

  /**
   * @param clientBuilder HTTP client builder to use for creating the client instance.
   */
  public AbstractHttpInterfaceManager(HttpClientBuilder clientBuilder) {
    this.clientBuilder = clientBuilder;
    this.lock = new Object();
  }

  @Override
  public void close() throws IOException {
    synchronized (lock) {
      closed = true;

      if (sharedClient != null) {
        CloseableHttpClient client = sharedClient;
        sharedClient = null;
        client.close();
      }
    }
  }

  protected CloseableHttpClient getSharedClient() {
    synchronized (lock) {
      if (closed) {
        throw new IllegalStateException("Cannot get http client for a closed manager.");
      }

      if (sharedClient == null) {
        sharedClient = clientBuilder.build();
      }

      return sharedClient;
    }
  }
}
