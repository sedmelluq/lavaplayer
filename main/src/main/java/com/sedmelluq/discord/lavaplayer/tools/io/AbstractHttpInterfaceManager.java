package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for an HTTP interface manager with lazily initialized http client instance.
 */
public abstract class AbstractHttpInterfaceManager implements HttpInterfaceManager {
  private static final Logger log = LoggerFactory.getLogger(AbstractHttpInterfaceManager.class);

  private final HttpClientBuilder clientBuilder;
  private final Object lock;
  private boolean closed;
  private CloseableHttpClient sharedClient;
  private RequestConfig requestConfig;

  /**
   * @param clientBuilder HTTP client builder to use for creating the client instance.
   * @param requestConfig Request config used by the client builder
   */
  public AbstractHttpInterfaceManager(HttpClientBuilder clientBuilder, RequestConfig requestConfig) {
    this.clientBuilder = clientBuilder;
    this.requestConfig = requestConfig;
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

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    synchronized (lock) {
      try {
        close();
      } catch (Exception e) {
        log.warn("Failed to close HTTP client.", e);
      }

      closed = false;
      requestConfig = configurator.apply(requestConfig);
      clientBuilder.setDefaultRequestConfig(requestConfig);
    }
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    synchronized (lock) {
      try {
        close();
      } catch (Exception e) {
        log.warn("Failed to close HTTP client.", e);
      }

      closed = false;
      configurator.accept(clientBuilder);
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
