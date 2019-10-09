package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.http.DualHttpRequestModifier;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpRequestModifier;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * HTTP interface manager which creates a new HTTP context for each interface.
 */
public class SimpleHttpInterfaceManager extends AbstractHttpInterfaceManager {
  private final DualHttpRequestModifier requestModifier;

  /**
   * @param clientBuilder HTTP client builder to use for creating the client instance.
   * @param requestConfig Request config used by the client builder
   * @param requestModifier
   */
  public SimpleHttpInterfaceManager(HttpClientBuilder clientBuilder, RequestConfig requestConfig,
                                    HttpRequestModifier requestModifier) {

    super(clientBuilder, requestConfig);
    this.requestModifier = new DualHttpRequestModifier(requestModifier);
  }

  @Override
  public HttpInterface getInterface() {
    return new HttpInterface(getSharedClient(), HttpClientContext.create(), false, requestModifier);
  }

  @Override
  public void setRequestModifier(HttpRequestModifier modifier) {
    requestModifier.setCustomModifier(modifier);
  }
}
