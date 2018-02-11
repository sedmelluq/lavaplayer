package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a class where HTTP request configuration can be changed.
 */
public interface HttpConfigurable {
  /**
   * @param configurator Function to reconfigure request config.
   */
  void configureRequests(Function<RequestConfig, RequestConfig> configurator);

  /**
   * @param configurator Function to reconfigure HTTP builder.
   */
  void configureBuilder(Consumer<HttpClientBuilder> configurator);
}
