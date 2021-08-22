package lavaplayer.tools.http

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext

interface HttpContextFilter {
    fun onContextOpen(context: HttpClientContext)

    fun onContextClose(context: HttpClientContext)

    fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean)

    fun onRequestResponse(context: HttpClientContext, request: HttpUriRequest, response: HttpResponse): Boolean

    fun onRequestException(context: HttpClientContext, request: HttpUriRequest, error: Throwable): Boolean
}
