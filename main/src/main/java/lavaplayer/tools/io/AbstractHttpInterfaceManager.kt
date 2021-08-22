package lavaplayer.tools.io;

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.function.Consumer
import java.util.function.Function

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
        private val logger: Logger = LoggerFactory.getLogger(AbstractHttpInterfaceManager::class.java);
    }

    private var closed: Boolean = false;
    private var sharedClient: CloseableHttpClient? = null;

    @Throws(IOException::class)
    override fun close() = synchronized(this) {
        closed = true;
        sharedClient?.use { sharedClient = null } ?: Unit
    }

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        synchronized(this) {
            try {
                close();
            } catch (e: Exception) {
                logger.warn("Failed to close HTTP client.", e);
            }

            closed = false;
            requestConfig = configurator.apply(requestConfig);
            clientBuilder.setDefaultRequestConfig(requestConfig);
        }
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        synchronized(this) {
            try {
                close();
            } catch (e: Exception) {
                logger.warn("Failed to close HTTP client.", e);
            }

            closed = false;
            configurator.accept(clientBuilder);
        }
    }

    protected fun getSharedClient() = synchronized(this) {
        if (closed) {
            throw IllegalStateException("Cannot get http client for a closed manager.");
        }

        if (sharedClient == null) {
            sharedClient = clientBuilder.build();
        }

        sharedClient!!
    }
}
