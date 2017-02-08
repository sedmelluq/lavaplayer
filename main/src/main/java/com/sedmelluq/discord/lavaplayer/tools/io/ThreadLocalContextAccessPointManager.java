package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

public class ThreadLocalContextAccessPointManager implements HttpAccessPointManager {
  private final HttpClientBuilder clientBuilder;
  private final ThreadLocal<HttpAccessPoint> accessPoints;
  private final Object lock;
  private boolean closed;
  private CloseableHttpClient sharedClient;

  public ThreadLocalContextAccessPointManager(HttpClientBuilder clientBuilder) {
    this.clientBuilder = clientBuilder;
    this.lock = new Object();

    this.accessPoints = ThreadLocal.withInitial(() ->
        new HttpAccessPoint(getSharedClient(), HttpClientContext.create(), false)
    );
  }

  @Override
  public HttpAccessPoint getAccessPoint() {
    HttpAccessPoint accessPoint = accessPoints.get();
    if (accessPoint.acquire()) {
      return accessPoint;
    }

    accessPoint = new HttpAccessPoint(getSharedClient(), HttpClientContext.create(), false);
    accessPoint.acquire();
    return accessPoint;
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

  private CloseableHttpClient getSharedClient() {
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
