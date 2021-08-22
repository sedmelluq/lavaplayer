package com.sedmelluq.discord.lavaplayer.tools.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class MultiHttpConfigurable implements ExtendedHttpConfigurable {
    private final Collection<ExtendedHttpConfigurable> configurables;

    public MultiHttpConfigurable(Collection<ExtendedHttpConfigurable> configurables) {
        this.configurables = configurables;
    }

    @Override
    public void setHttpContextFilter(HttpContextFilter filter) {
        for (ExtendedHttpConfigurable configurable : configurables) {
            configurable.setHttpContextFilter(filter);
        }
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        for (ExtendedHttpConfigurable configurable : configurables) {
            configurable.configureRequests(configurator);
        }
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        for (ExtendedHttpConfigurable configurable : configurables) {
            configurable.configureBuilder(configurator);
        }
    }
}
