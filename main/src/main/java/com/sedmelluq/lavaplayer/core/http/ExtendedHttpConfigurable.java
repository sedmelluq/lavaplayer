package com.sedmelluq.lavaplayer.core.http;

public interface ExtendedHttpConfigurable extends HttpConfigurable {
  void setHttpContextFilter(HttpContextFilter filter);
}
