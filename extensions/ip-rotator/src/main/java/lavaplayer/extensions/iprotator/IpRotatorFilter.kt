package lavaplayer.extensions.iprotator

import lavaplayer.extensions.iprotator.planner.AbstractRoutePlanner
import lavaplayer.extensions.iprotator.tools.RateLimitException
import lavaplayer.tools.http.AbstractHttpContextFilter
import lavaplayer.tools.http.HttpContextFilter
import lavaplayer.tools.http.HttpContextRetryCounter
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.slf4j.LoggerFactory
import java.net.BindException

class IpRotatorFilter(
    delegate: HttpContextFilter?,
    private val isSearch: Boolean,
    private val routePlanner: AbstractRoutePlanner,
    private val retryLimit: Int,
    retryCountAttribute: String = RETRY_COUNT_ATTRIBUTE
) : AbstractHttpContextFilter(delegate) {
    constructor(
        mainDelegate: HttpContextFilter?,
        isSearch: Boolean,
        routePlanner: AbstractRoutePlanner,
        retryLimit: Int
    ) : this(mainDelegate, isSearch, routePlanner, retryLimit, RETRY_COUNT_ATTRIBUTE)

    companion object {
        const val RETRY_COUNT_ATTRIBUTE = "retry-counter"

        private val log = LoggerFactory.getLogger(IpRotatorFilter::class.java)
    }

    private val retryCounter = HttpContextRetryCounter(retryCountAttribute)

    override fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean) {
        retryCounter.handleUpdate(context, isRepetition)
        super.onRequest(context, request, isRepetition)
    }

    override fun onRequestException(context: HttpClientContext, request: HttpUriRequest, error: Throwable): Boolean {
        if (error is BindException) {
            log.warn(
                "Cannot assign requested address {}, marking address as failing and retry!",
                routePlanner.getLastAddress(context)
            )
            routePlanner.markAddressFailing(context)
            return context.limitedRetry()
        }

        return super.onRequestException(context, request, error)
    }

    override fun onRequestResponse(
        context: HttpClientContext,
        request: HttpUriRequest,
        response: HttpResponse
    ): Boolean {
        if (isSearch) {
            if (response.isRateLimited) {
                if (routePlanner.shouldHandleSearchFailure()) {
                    log.warn("Search rate-limit reached, marking address as failing and retry")
                    routePlanner.markAddressFailing(context)
                }

                return context.limitedRetry()
            }
        } else if (response.isRateLimited) {
            log.warn(
                "Rate-limit reached, marking address {} as failing and retry",
                routePlanner.getLastAddress(context)
            )

            routePlanner.markAddressFailing(context)
            return context.limitedRetry()
        }

        return super.onRequestResponse(context, request, response)
    }

    private val HttpResponse.isRateLimited: Boolean
        get() = statusLine.statusCode == 429

    private fun HttpClientContext.limitedRetry(): Boolean {
        return if (retryCounter.retryCountFor(this) >= retryLimit) {
            throw RateLimitException("Retry aborted, too many retries on rate-limit.")
        } else {
            true
        }
    }
}
