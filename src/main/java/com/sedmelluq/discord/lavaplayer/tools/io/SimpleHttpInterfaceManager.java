package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * HTTP interface manager which creates a new HTTP context for each interface.
 */
public class SimpleHttpInterfaceManager extends AbstractHttpInterfaceManager {
  /**
   * @param clientBuilder HTTP client builder to use for creating the client instance.
   * @param requestConfig Request config used by the client builder
   */
  public SimpleHttpInterfaceManager(HttpClientBuilder clientBuilder, RequestConfig requestConfig) {
    super(clientBuilder, requestConfig);
  }

  @Override
  public HttpInterface getInterface() {
    return new HttpInterface(getSharedClient(), HttpClientContext.create(), false);
  }
}
