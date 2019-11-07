package com.sedmelluq.lava.extensions.youtuberotator;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.RateLimitException;
import java.net.BindException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YoutubeIpRotator extends YoutubeHttpContextFilter {
  private static final Logger log = LoggerFactory.getLogger(YoutubeIpRotator.class);

  private static final String RETRY_COUNT_ATTRIBUTE = "yt-retry-counter";
  private static final int DEFAULT_RETRY_LIMIT = 4;

  private final boolean isSearch;
  private final AbstractRoutePlanner routePlanner;
  private final int retryLimit;

  private YoutubeIpRotator(boolean isSearch, AbstractRoutePlanner routePlanner, int retryLimit) {
    this.isSearch = isSearch;
    this.routePlanner = routePlanner;
    this.retryLimit = retryLimit;
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
    if (isRepetition) {
      setRetryCount(context, getRetryCount(context) + 1);
    } else {
      setRetryCount(context, 0);
    }

    super.onRequest(context, request, isRepetition);
  }

  @Override
  public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
    int statusCode = response.getStatusLine().getStatusCode();

    if (isSearch) {
      if (statusCode == 429) {
        if (routePlanner.shouldHandleSearchFailure()) {
          log.warn("YouTube search rate limit reached, marking address as failing and retry");
          routePlanner.markAddressFailing(context);
        }

        return limitedRetry(context);
      }
    } else if (isRateLimited(request, response)) {
      log.warn("YouTube rate limit reached, marking address {} as failing and retry",
          routePlanner.getLastAddress(context));
      routePlanner.markAddressFailing(context);

      return limitedRetry(context);
    }

    return super.onRequestResponse(context, request, response);
  }

  private boolean isRateLimited(HttpUriRequest request, HttpResponse response) {
    int statusCode = response.getStatusLine().getStatusCode();

    if (statusCode == 429) {
      return true;
    } else if (request.getURI().toString().contains("pbj=1")) {
      Header contentType = response.getFirstHeader("content-type");
      return contentType != null && contentType.getValue().contains("text/html");
    }

    return false;
  }

  @Override
  public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
    if (error instanceof BindException) {
      log.warn("Cannot assign requested address {}, marking address as failing and retry!",
          routePlanner.getLastAddress(context));

      routePlanner.markAddressFailing(context);
      return true;
    }

    return super.onRequestException(context, request, error);
  }

  public static void setup(YoutubeAudioSourceManager sourceManager, AbstractRoutePlanner routePlanner) {
    setup(sourceManager, routePlanner, DEFAULT_RETRY_LIMIT);
  }

  public static void setup(YoutubeAudioSourceManager sourceManager, AbstractRoutePlanner routePlanner, int retryLimit) {
    sourceManager.configureBuilder(it ->
        it.setRoutePlanner(routePlanner)
    );

    sourceManager
        .getMainHttpConfiguration()
        .setHttpContextFilter(new YoutubeIpRotator(false, routePlanner, retryLimit));

    sourceManager
        .getSearchHttpConfiguration()
        .setHttpContextFilter(new YoutubeIpRotator(true, routePlanner, retryLimit));
  }

  public static void setup(AudioPlayerManager playerManager, AbstractRoutePlanner routePlanner) {
    YoutubeAudioSourceManager sourceManager = playerManager.source(YoutubeAudioSourceManager.class);

    if (sourceManager != null) {
      setup(sourceManager, routePlanner, DEFAULT_RETRY_LIMIT);
    }
  }

  public static void setup(AudioPlayerManager playerManager, AbstractRoutePlanner routePlanner, int retryLimit) {
    YoutubeAudioSourceManager sourceManager = playerManager.source(YoutubeAudioSourceManager.class);

    if (sourceManager != null) {
      setup(sourceManager, routePlanner, retryLimit);
    }
  }

  private boolean limitedRetry(HttpClientContext context) {
    if (getRetryCount(context) >= retryLimit) {
      throw new RateLimitException("Retry aborted, too many retries on ratelimit.");
    } else {
      return true;
    }
  }

  private void setRetryCount(HttpClientContext context, int value) {
    RetryCount count = context.getAttribute(RETRY_COUNT_ATTRIBUTE, RetryCount.class);

    if (count == null) {
      count = new RetryCount();
      context.setAttribute(RETRY_COUNT_ATTRIBUTE, count);
    }

    count.value = value;
  }

  private int getRetryCount(HttpClientContext context) {
    RetryCount count = context.getAttribute(RETRY_COUNT_ATTRIBUTE, RetryCount.class);
    return count != null ? count.value : 0;
  }

  private static class RetryCount {
    private int value;
  }
}
