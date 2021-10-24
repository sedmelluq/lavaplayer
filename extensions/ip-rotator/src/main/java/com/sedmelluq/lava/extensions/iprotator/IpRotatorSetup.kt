package com.sedmelluq.lava.extensions.iprotator

import com.sedmelluq.lava.extensions.iprotator.planner.AbstractRoutePlanner
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpClientBuilder
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable
import com.sedmelluq.discord.lavaplayer.tools.http.SimpleHttpClientConnectionManager

abstract class IpRotatorSetup(protected val routePlanner: AbstractRoutePlanner) {
    /**
     * The retry handler to use.
     */
    abstract val retryHandler: IpRotatorRetryHandler

    /**
     * Setup ip-rotation.
     */
    abstract fun setup()

    /**
     * Applies this ip-rotator-setup to the supplied list of [ExtendedHttpConfigurable]
     *
     * @param configurations The list of [ExtendedHttpConfigurable] to configure.
     * @param filter The ip-rotator filter to use.
     */
    protected fun apply(configurations: List<ExtendedHttpConfigurable>, filter: IpRotatorFilter? = null) {
        for (configurable in configurations) {
            configurable.configureBuilder {
                (this as ExtendedHttpClientBuilder).setConnectionManagerFactory(::SimpleHttpClientConnectionManager)
            }

            configurable.configureBuilder {
                setRoutePlanner(routePlanner)

                // No retry for some exceptions we know are hopeless for retry.
                setRetryHandler(retryHandler)

                // Regularly cleans up per-route connection pool which gets huge due to many routes caused by
                // each request having a unique route.
                evictExpiredConnections()
            }

            filter?.let { configurable.setHttpContextFilter(it) }
        }
    }
}
