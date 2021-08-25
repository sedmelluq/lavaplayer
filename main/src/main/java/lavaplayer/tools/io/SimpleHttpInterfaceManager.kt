package lavaplayer.tools.io

import lavaplayer.tools.http.HttpContextFilter
import lavaplayer.tools.http.SettableHttpRequestFilter
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.HttpClientBuilder

/**
 * HTTP interface manager which creates a new HTTP context for each interface.
 * @param clientBuilder HTTP client builder to use for creating the client instance.
 * @param requestConfig Request config used by the client builder
 */
class SimpleHttpInterfaceManager(
    clientBuilder: HttpClientBuilder,
    requestConfig: RequestConfig
) : AbstractHttpInterfaceManager(clientBuilder, requestConfig) {
    private val filterHolder = SettableHttpRequestFilter()

    override fun get(): HttpInterface {
        val httpInterface = HttpInterface(getSharedClient(), HttpClientContext.create(), false, filterHolder)
        httpInterface.acquire()
        return httpInterface
    }

    override fun setHttpContextFilter(filter: HttpContextFilter) {
        filterHolder.set(filter)
    }
}
