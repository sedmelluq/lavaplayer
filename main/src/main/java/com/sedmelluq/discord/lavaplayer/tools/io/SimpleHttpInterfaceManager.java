package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * HTTP interface manager which creates a new HTTP context for each interface.
 */
public class SimpleHttpInterfaceManager extends AbstractHttpInterfaceManager {
  /**
   * @param clientBuilder HTTP client builder to use for creating the client instance.
   */
  public SimpleHttpInterfaceManager(HttpClientBuilder clientBuilder) {
    super(clientBuilder);
  }

  @Override
  public HttpInterface getInterface() {
    return new HttpInterface(getSharedClient(), HttpClientContext.create(), false);
  }
}
