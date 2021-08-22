package com.sedmelluq.discord.lavaplayer.tools.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;

public interface HttpContextFilter {
    void onContextOpen(HttpClientContext context);

    void onContextClose(HttpClientContext context);

    void onRequest(HttpClientContext context, HttpUriRequest request, boolean isRepetition);

    boolean onRequestResponse(HttpClientContext context, HttpUriRequest request, HttpResponse response);

    boolean onRequestException(HttpClientContext context, HttpUriRequest request, Throwable error);
}
