package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.http.SettableHttpRequestFilter;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * HTTP interface manager which creates a new HTTP context for each interface.
 */
public class SimpleHttpInterfaceManager extends AbstractHttpInterfaceManager {
    private final SettableHttpRequestFilter filterHolder;

    /**
     * @param clientBuilder HTTP client builder to use for creating the client instance.
     * @param requestConfig Request config used by the client builder
     */
    public SimpleHttpInterfaceManager(HttpClientBuilder clientBuilder, RequestConfig requestConfig) {
        super(clientBuilder, requestConfig);
        this.filterHolder = new SettableHttpRequestFilter();
    }

    @Override
    public HttpInterface getInterface() {
        HttpInterface httpInterface = new HttpInterface(getSharedClient(), HttpClientContext.create(), false, filterHolder);
        httpInterface.acquire();
        return httpInterface;
    }

    @Override
    public void setHttpContextFilter(HttpContextFilter filter) {
        filterHolder.set(filter);
    }
}
