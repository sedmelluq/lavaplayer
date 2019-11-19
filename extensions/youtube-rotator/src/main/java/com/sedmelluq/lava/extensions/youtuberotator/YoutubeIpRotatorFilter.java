package com.sedmelluq.lava.extensions.youtuberotator;

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.RateLimitException;
import java.net.BindException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YoutubeIpRotatorFilter implements HttpContextFilter {
  private static final Logger log = LoggerFactory.getLogger(YoutubeIpRotatorFilter.class);

  private static final String RETRY_COUNT_ATTRIBUTE = "yt-retry-counter";

  private final HttpContextFilter delegate;
  private final boolean isSearch;
  private final AbstractRoutePlanner routePlanner;
  private final int retryLimit;

  public YoutubeIpRotatorFilter(
      HttpContextFilter delegate,
      boolean isSearch,
      AbstractRoutePlanner routePlanner,
      int retryLimit
  ) {
    this.delegate = delegate;
    this.isSearch = isSearch;
    this.routePlanner = routePlanner;
    this.retryLimit = retryLimit;
  }

  @Override
  public void onContextOpen(HttpClientContext context) {
    if (delegate != null) {
      delegate.onContextOpen(context);
    }
  }

  @Override
  public void onContextClose(HttpClientContext context) {
    if (delegate != null) {
      delegate.onContextClose(context);
    }
  }

  @Override
  public void onRequest(HttpClientContext context, HttpUriRequest request, boolean isRepetition) {
    if (isRepetition) {
      setRetryCount(context, getRetryCount(context) + 1);
    } else {
      setRetryCount(context, 0);
    }

    if (delegate != null) {
      delegate.onRequest(context, request, isRepetition);
    }
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
    } else if (isRateLimited(response)) {
      log.warn("YouTube rate limit reached, marking address {} as failing and retry",
          routePlanner.getLastAddress(context));
      routePlanner.markAddressFailing(context);

      return limitedRetry(context);
    }

    if (delegate != null) {
      return delegate.onRequestResponse(context, request, response);
    } else {
      return false;
    }
  }

  private boolean isRateLimited(HttpResponse response) {
    int statusCode = response.getStatusLine().getStatusCode();
    return statusCode == 429;
  }

  @Override
  public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
    if (error instanceof BindException) {
      log.warn("Cannot assign requested address {}, marking address as failing and retry!",
          routePlanner.getLastAddress(context));

      routePlanner.markAddressFailing(context);
      return limitedRetry(context);
    }

    if (delegate != null) {
      return delegate.onRequestException(context, request, error);
    } else {
      return false;
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
