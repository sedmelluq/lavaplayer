package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * An HTTP interface for performing HTTP requests in one specific thread. This also means it is not thread safe and should
 * not be used in a thread it was not obtained in. For multi-thread use {@link HttpInterfaceManager#getInterface()},
 * should be called in each thread separately.
 */
public class HttpInterface implements Closeable {
    private final CloseableHttpClient client;
    private final HttpClientContext context;
    private final boolean ownedClient;
    private final HttpContextFilter filter;
    private HttpUriRequest lastRequest;
    private boolean available;

    /**
     * @param client      The http client instance used.
     * @param context     The http context instance used.
     * @param ownedClient True if the client should be closed when this instance is closed.
     * @param filter
     */
    public HttpInterface(CloseableHttpClient client, HttpClientContext context, boolean ownedClient,
                         HttpContextFilter filter) {

        this.client = client;
        this.context = context;
        this.ownedClient = ownedClient;
        this.filter = filter;
        this.available = true;
    }

    /**
     * Acquire exclusive use of this instance. This is released by calling close.
     *
     * @return True if this instance was not exclusively used when this method was called.
     */
    public boolean acquire() {
        if (!available) {
            return false;
        }

        filter.onContextOpen(context);
        available = false;
        return true;
    }

    /**
     * Executes the given query using the client and context stored in this instance.
     *
     * @param request The request to execute.
     * @return Closeable response from the server.
     * @throws IOException On network error.
     */
    public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
        boolean isRepeated = false;

        while (true) {
            filter.onRequest(context, request, isRepeated);

            try {
                CloseableHttpResponse response = client.execute(request, context);
                lastRequest = request;

                if (!filter.onRequestResponse(context, request, response)) {
                    return response;
                }
            } catch (Throwable e) {
                if (!filter.onRequestException(context, request, e)) {
                    if (e instanceof Error) {
                        throw (Error) e;
                    } else if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else //noinspection ConstantConditions
                        if (e instanceof IOException) {
                            throw (IOException) e;
                        } else {
                            throw new RuntimeException(e);
                        }
                } else {
                    ExceptionTools.rethrowErrors(e);
                }
            }

            isRepeated = true;
        }
    }

    /**
     * @return The final URL after redirects for the last processed request. Original URL if no redirects were performed.
     * Null if no requests have been executed. Undefined state if last request threw an exception.
     */
    public URI getFinalLocation() {
        List<URI> redirectLocations = context.getRedirectLocations();

        if (redirectLocations != null && !redirectLocations.isEmpty()) {
            return redirectLocations.get(redirectLocations.size() - 1);
        } else {
            return lastRequest != null ? lastRequest.getURI() : null;
        }
    }

    /**
     * @return Http client context used by this interface.
     */
    public HttpClientContext getContext() {
        return context;
    }

    /**
     * @return Http client instance used by this instance.
     */
    public CloseableHttpClient getHttpClient() {
        return client;
    }

    @Override
    public void close() throws IOException {
        available = true;
        filter.onContextClose(context);

        if (ownedClient) {
            client.close();
        }
    }
}
