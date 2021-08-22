package lavaplayer.tools.io

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder

import java.util.function.Function

import java.util.function.Consumer

/**
 * Represents a class where HTTP request configuration can be changed.
 */
interface HttpConfigurable {
    /**
     * @param configurator Function to reconfigure request config.
     */
    fun configureRequests(configurator: Function<RequestConfig, RequestConfig>)

    /**
     * @param configurator Function to reconfigure HTTP builder.
     */
    fun configureBuilder(configurator: Consumer<HttpClientBuilder>)
}
