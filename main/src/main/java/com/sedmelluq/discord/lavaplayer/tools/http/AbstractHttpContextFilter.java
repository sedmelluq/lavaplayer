package com.sedmelluq.discord.lavaplayer.tools.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;

public abstract class AbstractHttpContextFilter implements HttpContextFilter {
    private final HttpContextFilter delegate;

    protected AbstractHttpContextFilter(HttpContextFilter delegate) {
        this.delegate = delegate;
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
        if (delegate != null) {
            delegate.onRequest(context, request, isRepetition);
        }
    }

    @Override
    public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
        if (delegate != null) {
            return delegate.onRequestResponse(context, request, response);
        }

        return false;
    }

    @Override
    public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
        if (delegate != null) {
            return delegate.onRequestException(context, request, error);
        }

        return false;
    }
}
