package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * HTTP interface manager which reuses an HttpContext by keeping it as a thread local. In case a new interface is
 * requested before the previous one has been closed, it creates a new context for the returned interface. The HTTP
 * client instance used is created lazily.
 */
public class ThreadLocalHttpInterfaceManager extends AbstractHttpInterfaceManager {
  private final ThreadLocal<HttpInterface> httpInterfaces;

  /**
   * @param clientBuilder HTTP client builder to use for creating the client instance.
   */
  public ThreadLocalHttpInterfaceManager(HttpClientBuilder clientBuilder) {
    super(clientBuilder);

    this.httpInterfaces = ThreadLocal.withInitial(() ->
        new HttpInterface(getSharedClient(), HttpClientContext.create(), false)
    );
  }

  @Override
  public HttpInterface getInterface() {
    HttpInterface httpInterface = httpInterfaces.get();
    if (httpInterface.acquire()) {
      return httpInterface;
    }

    httpInterface = new HttpInterface(getSharedClient(), HttpClientContext.create(), false);
    httpInterface.acquire();
    return httpInterface;
  }
}
