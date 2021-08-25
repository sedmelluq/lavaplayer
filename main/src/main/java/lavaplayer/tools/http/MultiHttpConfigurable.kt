package lavaplayer.tools.http

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder

import java.util.function.Consumer
import java.util.function.Function

open class MultiHttpConfigurable(private val configurables: Collection<ExtendedHttpConfigurable>) : ExtendedHttpConfigurable {
    override fun setHttpContextFilter(filter: HttpContextFilter) {
        for (configurable in configurables) {
            configurable.setHttpContextFilter(filter)
        }
    }

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        for (configurable in configurables) {
            configurable.configureRequests(configurator)
        }
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        for (configurable in configurables) {
            configurable.configureBuilder(configurator)
        }
    }
}
