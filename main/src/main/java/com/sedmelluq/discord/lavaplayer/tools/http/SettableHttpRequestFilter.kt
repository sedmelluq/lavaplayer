package com.sedmelluq.discord.lavaplayer.tools.http

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext

open class SettableHttpRequestFilter : HttpContextFilter {
    private var filter: HttpContextFilter? = null

    fun get(): HttpContextFilter? {
        return filter
    }

    fun set(filter: HttpContextFilter) {
        this.filter = filter
    }

    override fun onContextOpen(context: HttpClientContext) {
        filter?.onContextOpen(context)
    }

    override fun onContextClose(context: HttpClientContext) {
        filter?.onContextClose(context)
    }

    override fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean) {
        filter?.onRequest(context, request, isRepetition)
    }

    override fun onRequestResponse(
        context: HttpClientContext,
        request: HttpUriRequest,
        response: HttpResponse
    ): Boolean {
        return filter?.onRequestResponse(context, request, response) ?: false
    }

    override fun onRequestException(context: HttpClientContext, request: HttpUriRequest, error: Throwable): Boolean {
        return filter?.onRequestException(context, request, error) ?: false
    }
}
