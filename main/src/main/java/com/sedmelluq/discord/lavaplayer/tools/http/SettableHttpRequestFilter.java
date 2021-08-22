package com.sedmelluq.discord.lavaplayer.tools.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;

public class SettableHttpRequestFilter implements HttpContextFilter {
    private HttpContextFilter filter;

    public HttpContextFilter get() {
        return filter;
    }

    public void set(HttpContextFilter filter) {
        this.filter = filter;
    }

    @Override
    public void onContextOpen(HttpClientContext context) {
        HttpContextFilter current = filter;

        if (current != null) {
            current.onContextOpen(context);
        }
    }

    @Override
    public void onContextClose(HttpClientContext context) {
        HttpContextFilter current = filter;

        if (current != null) {
            current.onContextClose(context);
        }
    }

    @Override
    public void onRequest(HttpClientContext context, HttpUriRequest request, boolean isRepetition) {
        HttpContextFilter current = filter;

        if (current != null) {
            current.onRequest(context, request, isRepetition);
        }
    }

    @Override
    public boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response) {
        HttpContextFilter current = filter;

        if (current != null) {
            return current.onRequestResponse(context, request, response);
        } else {
            return false;
        }
    }

    @Override
    public boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error) {
        HttpContextFilter current = filter;

        if (current != null) {
            return current.onRequestException(context, request, error);
        } else {
            return false;
        }
    }
}
