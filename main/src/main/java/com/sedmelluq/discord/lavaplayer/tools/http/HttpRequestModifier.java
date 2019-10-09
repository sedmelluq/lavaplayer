package com.sedmelluq.discord.lavaplayer.tools.http;

import org.apache.http.client.methods.HttpUriRequest;

public interface HttpRequestModifier {
  void modify(HttpUriRequest request);
}
