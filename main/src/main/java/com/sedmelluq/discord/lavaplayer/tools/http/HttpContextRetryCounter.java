package com.sedmelluq.discord.lavaplayer.tools.http;

import org.apache.http.client.protocol.HttpClientContext;

public class HttpContextRetryCounter {
    private final String attributeName;

    public HttpContextRetryCounter(String attributeName) {
        this.attributeName = attributeName;
    }

    public void handleUpdate(HttpClientContext context, boolean isRepetition) {
        if (isRepetition) {
            setRetryCount(context, getRetryCount(context) + 1);
        } else {
            setRetryCount(context, 0);
        }
    }

    public void setRetryCount(HttpClientContext context, int value) {
        RetryCount count = context.getAttribute(attributeName, RetryCount.class);

        if (count == null) {
            count = new RetryCount();
            context.setAttribute(attributeName, count);
        }

        count.value = value;
    }

    public int getRetryCount(HttpClientContext context) {
        RetryCount count = context.getAttribute(attributeName, RetryCount.class);
        return count != null ? count.value : 0;
    }

    private static class RetryCount {
        private int value;
    }
}
