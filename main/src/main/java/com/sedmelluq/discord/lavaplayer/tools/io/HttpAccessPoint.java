package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.Closeable;
import java.io.IOException;

/**
 * An access point for performing HTTP requests in a single thread. Not thread safe, for asynchronous usage, always
 * call {@link HttpAccessPointManager#getAccessPoint()} to get a new instance in each thread.
 */
public class HttpAccessPoint implements Closeable {
  private final CloseableHttpClient client;
  private final HttpContext context;
  private final boolean ownedClient;
  private boolean available;

  public HttpAccessPoint(CloseableHttpClient client, HttpContext context, boolean ownedClient) {
    this.client = client;
    this.context = context;
    this.ownedClient = ownedClient;
    this.available = true;
  }

  public boolean acquire() {
    if (!available) {
      return false;
    }

    available = false;
    return true;
  }

  public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
    return client.execute(request, context);
  }

  @Override
  public void close() throws IOException {
    available = true;

    if (ownedClient) {
      client.close();
    }
  }
}
