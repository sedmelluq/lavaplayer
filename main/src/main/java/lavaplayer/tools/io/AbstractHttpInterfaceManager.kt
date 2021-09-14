package lavaplayer.tools.io

import mu.KotlinLogging
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import java.io.IOException

/**
 * Base class for an HTTP interface manager with lazily initialized http client instance.
 * @param clientBuilder HTTP client builder to use for creating the client instance.
 * @param requestConfig Request config used by the client builder
 */
abstract class AbstractHttpInterfaceManager(
    private val clientBuilder: HttpClientBuilder,
    private var requestConfig: RequestConfig
) : HttpInterfaceManager {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private var closed: Boolean = false
    private var sharedClient: CloseableHttpClient? = null

    @Throws(IOException::class)
    override fun close() = synchronized(this) {
        closed = true
        sharedClient?.use { sharedClient = null } ?: Unit
    }

    override fun configureRequests(configurator: RequestConfigurator) {
        synchronized(this) {
            try {
                close()
            } catch (e: Exception) {
                logger.warn(e) { "Failed to close HTTP client." }
            }

            closed = false
            requestConfig = configurator(requestConfig)
            clientBuilder.setDefaultRequestConfig(requestConfig)
        }
    }

    override fun configureBuilder(configurator: BuilderConfigurator) {
        synchronized(this) {
            try {
                close()
            } catch (e: Exception) {
                logger.warn(e) { "Failed to close HTTP client." }
            }

            closed = false
            clientBuilder.apply(configurator)
        }
    }

    protected fun getSharedClient() = synchronized(this) {
        if (closed) {
            throw IllegalStateException("Cannot get http client for a closed manager.")
        }

        if (sharedClient == null) {
            sharedClient = clientBuilder.build()
        }

        sharedClient!!
    }
}
