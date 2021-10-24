package com.sedmelluq.discord.lavaplayer.tools.io

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder

/**
 * Function to reconfigure a [RequestConfig].
 */
typealias RequestConfigurator = RequestConfig.() -> RequestConfig

/**
 * Function to reconfigure an [HttpClientBuilder].
 */
typealias BuilderConfigurator = HttpClientBuilder.() -> Unit

/**
 * Represents a class where HTTP request configuration can be changed.
 */
interface HttpConfigurable {
    /**
     * @param configurator Function to reconfigure request config.
     */
    fun configureRequests(configurator: RequestConfigurator)

    /**
     * @param configurator Function to reconfigure HTTP builder.
     */
    fun configureBuilder(configurator: BuilderConfigurator)
}
