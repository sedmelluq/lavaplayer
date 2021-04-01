package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;

public class BaseYoutubeHttpContextFilter implements HttpContextFilter {
  @Override
  public void onContextOpen(HttpClientContext context) {

  }

  @Override
  public void onContextClose(HttpClientContext context) {

  }

  @Override
  public void onRequest(HttpClientContext context, HttpUriRequest request, boolean isRepetition) {
    // Consent cookie, so we do not land on consent page for HTML requests
    request.addHeader("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+471");
  }

  @Override
  public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
    return false;
  }

  @Override
  public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
    return false;
  }
}
