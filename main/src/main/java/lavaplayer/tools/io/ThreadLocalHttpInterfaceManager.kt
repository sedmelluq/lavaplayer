package lavaplayer.tools.io

import lavaplayer.tools.http.HttpContextFilter
import lavaplayer.tools.http.SettableHttpRequestFilter
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.HttpClientBuilder

/**
 * HTTP interface manager which reuses an HttpContext by keeping it as a thread local. In case a new interface is
 * requested before the previous one has been closed, it creates a new context for the returned interface. The HTTP
 * client instance used is created lazily.
 *
 * @param clientBuilder HTTP client builder to use for creating the client instance.
 * @param requestConfig Request config used by the client builder
 */
class ThreadLocalHttpInterfaceManager(
    clientBuilder: HttpClientBuilder,
    requestConfig: RequestConfig
) : AbstractHttpInterfaceManager(clientBuilder, requestConfig) {
    private val httpInterfaces: ThreadLocal<HttpInterface>
    private val filter = SettableHttpRequestFilter()

    init {
        httpInterfaces = ThreadLocal.withInitial { HttpInterface(getSharedClient(), HttpClientContext.create(), false, filter) }
    }

    override fun get(): HttpInterface {
        val client = getSharedClient()

        var httpInterface = httpInterfaces.get()
        if (httpInterface.httpClient !== client) {
            httpInterfaces.remove()
            httpInterface = httpInterfaces.get()
        }

        if (httpInterface.acquire()) {
            return httpInterface
        }

        httpInterface = HttpInterface(client, HttpClientContext.create(), false, filter)
        httpInterface.acquire()
        return httpInterface
    }

    override fun setHttpContextFilter(filter: HttpContextFilter) {
        this.filter.set(filter)
    }
}
