package com.sedmelluq.lava.extensions.youtuberotator;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

public class YoutubeIpRotatorRetryHandler implements HttpRequestRetryHandler {
    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        if (exception instanceof BindException) {
            return false;
        } else if (exception instanceof SocketException) {
            String message = exception.getMessage();

            if (message != null && message.contains("Protocol family unavailable")) {
                return false;
            }
        }

        return DefaultHttpRequestRetryHandler.INSTANCE.retryRequest(exception, executionCount, context);
    }
}
