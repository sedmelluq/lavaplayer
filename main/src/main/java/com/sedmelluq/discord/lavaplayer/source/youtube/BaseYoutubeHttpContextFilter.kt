package com.sedmelluq.discord.lavaplayer.source.youtube

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext

open class BaseYoutubeHttpContextFilter : HttpContextFilter {
    override fun onContextOpen(context: HttpClientContext) {

    }

    override fun onContextClose(context: HttpClientContext) {

    }

    override fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean) {
        // Consent cookie, so we do not land on consent page for HTML requests
        request.addHeader("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+471")
    }

    override fun onRequestResponse(
        context: HttpClientContext,
        request: HttpUriRequest,
        response: HttpResponse
    ): Boolean {
        return false
    }

    override fun onRequestException(context: HttpClientContext, request: HttpUriRequest, error: Throwable): Boolean {
        return false
    }
}
