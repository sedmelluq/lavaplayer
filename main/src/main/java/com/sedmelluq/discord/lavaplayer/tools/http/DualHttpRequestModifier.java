package com.sedmelluq.discord.lavaplayer.tools.http;

import org.apache.http.client.methods.HttpUriRequest;

public class DualHttpRequestModifier implements HttpRequestModifier {
  private final HttpRequestModifier baseModifier;
  private HttpRequestModifier customModifier;

  public DualHttpRequestModifier(HttpRequestModifier baseModifier) {
    this.baseModifier = baseModifier;
  }

  public void setCustomModifier(HttpRequestModifier customModifier) {
    this.customModifier = customModifier;
  }

  @Override
  public void modify(HttpUriRequest request) {
    if (baseModifier != null) {
      baseModifier.modify(request);
    }

    HttpRequestModifier custom = customModifier;

    if (custom != null) {
      custom.modify(request);
    }
  }
}
