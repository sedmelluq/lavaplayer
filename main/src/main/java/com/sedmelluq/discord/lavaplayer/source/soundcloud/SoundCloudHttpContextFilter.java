package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextRetryCounter;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;

public class SoundCloudHttpContextFilter implements HttpContextFilter {
  private static final HttpContextRetryCounter retryCounter = new HttpContextRetryCounter("sc-id-retry");

  private final SoundCloudClientIdTracker clientIdTracker;

  public SoundCloudHttpContextFilter(SoundCloudClientIdTracker clientIdTracker) {
    this.clientIdTracker = clientIdTracker;
  }

  @Override
  public void onContextOpen(HttpClientContext context) {

  }

  @Override
  public void onContextClose(HttpClientContext context) {

  }

  @Override
  public void onRequest(HttpClientContext context, HttpUriRequest request, boolean isRepetition) {
    retryCounter.handleUpdate(context, isRepetition);

    if (clientIdTracker.isIdFetchContext(context)) {
      // Used for fetching client ID, let's not recurse.
      return;
    } else if (request.getURI().getHost().contains("sndcdn.com")) {
      // CDN urls do not require client ID (it actually breaks them)
      return;
    }

    try {
      URI uri = new URIBuilder(request.getURI())
          .setParameter("client_id", clientIdTracker.getClientId())
          .build();

      if (request instanceof HttpRequestBase) {
        ((HttpRequestBase) request).setURI(uri);
      } else {
        throw new IllegalStateException("Cannot update request URI.");
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
    if (clientIdTracker.isIdFetchContext(context) || retryCounter.getRetryCount(context) >= 1) {
      return false;
    } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
      clientIdTracker.updateClientId();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
    return false;
  }
}
