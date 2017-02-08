package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.Closeable;
import java.io.IOException;

/**
 * An HTTP interface for performing HTTP requests in one specific thread. This also means it is not thread safe and should
 * not be used in a thread it was not obtained in. For multi-thread use {@link HttpInterfaceManager#getInterface()},
 * should be called in each thread separately.
 */
public class HttpInterface implements Closeable {
  private final CloseableHttpClient client;
  private final HttpContext context;
  private final boolean ownedClient;
  private boolean available;

  /**
   * @param client The http client instance used.
   * @param context The http context instance used.
   * @param ownedClient True if the client should be closed when this instance is closed.
   */
  public HttpInterface(CloseableHttpClient client, HttpContext context, boolean ownedClient) {
    this.client = client;
    this.context = context;
    this.ownedClient = ownedClient;
    this.available = true;
  }

  /**
   * Acquire exclusive use of this instance. This is released by calling close.
   *
   * @return True if this instance was not exclusively used when this method was called.
   */
  public boolean acquire() {
    if (!available) {
      return false;
    }

    available = false;
    return true;
  }

  /**
   * Executes the given query using the client and context stored in this instance.
   *
   * @param request The request to execute.
   * @return Closeable response from the server.
   * @throws IOException On network error.
   */
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
