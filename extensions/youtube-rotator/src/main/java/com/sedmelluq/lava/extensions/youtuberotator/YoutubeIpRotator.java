package com.sedmelluq.lava.extensions.youtuberotator;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.RateLimitException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;

public class YoutubeIpRotator extends YoutubeHttpContextFilter {
  private static final Logger log = LoggerFactory.getLogger(YoutubeIpRotator.class);

  private final boolean isSearch;
  private final AbstractRoutePlanner routePlanner;

  private YoutubeIpRotator(boolean isSearch, AbstractRoutePlanner routePlanner) {
    this.isSearch = isSearch;
    this.routePlanner = routePlanner;
  }

  @Override
  public void onContextOpen(HttpClientContext context) {
    super.onContextOpen(context);
  }

  @Override
  public void onContextClose(HttpClientContext context) {
    super.onContextClose(context);
  }

  @Override
  public void onRequest(HttpClientContext context, HttpUriRequest request, boolean isRepetition) {
    super.onRequest(context, request, isRepetition);
  }

  @Override
  public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
    if (isSearch) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == 429) {
        if (routePlanner.shouldHandleSearchFailure()) {
          log.warn("YouTube search rate limit reached, marking address as failing and retry");
          routePlanner.markAddressFailing();
          return true;
        }
        throw new RateLimitException("YouTube search rate limit reached");
      }
    } else {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 429) {
        log.warn("YouTube search rate limit reached, marking address as failing and retry");
        routePlanner.markAddressFailing();
        return true;
      }
    }

    return super.onRequestResponse(context, request, response);
  }

  @Override
  public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
    if (error instanceof BindException) {
      log.warn("Cannot assign requested address {}, marking address as failing and retry!", routePlanner.getLastAddress());
      routePlanner.markAddressFailing();
      return true;
    }

    return super.onRequestException(context, request, error);
  }

  public static void setup(YoutubeAudioSourceManager sourceManager, AbstractRoutePlanner routePlanner) {
    sourceManager.configureBuilder(it ->
        it.setRoutePlanner(routePlanner)
    );

    sourceManager
        .getMainHttpConfiguration()
        .setHttpContextFilter(new YoutubeIpRotator(false, routePlanner));

    sourceManager
        .getSearchHttpConfiguration()
        .setHttpContextFilter(new YoutubeIpRotator(true, routePlanner));
  }

  public static void setup(AudioPlayerManager playerManager, AbstractRoutePlanner routePlanner) {
    YoutubeAudioSourceManager sourceManager = playerManager.source(YoutubeAudioSourceManager.class);

    if (sourceManager != null) {
      setup(sourceManager, routePlanner);
    }
  }
}
