package lavaplayer.tools.http

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext

abstract class AbstractHttpContextFilter(private val delegate: HttpContextFilter?) : HttpContextFilter {
    override fun onContextOpen(context: HttpClientContext) {
        delegate?.onContextOpen(context)
    }

    override fun onContextClose(context: HttpClientContext) {
        delegate?.onContextClose(context)
    }

    override fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean) {
        delegate?.onRequest(context, request, isRepetition)
    }

    override fun onRequestResponse(
        context: HttpClientContext,
        request: HttpUriRequest,
        response: HttpResponse
    ): Boolean {
        return delegate?.onRequestResponse(context, request, response) ?: false
    }

    override fun onRequestException(context: HttpClientContext, request: HttpUriRequest, error: Throwable): Boolean {
        return delegate?.onRequestException(context, request, error) ?: false
    }
}
